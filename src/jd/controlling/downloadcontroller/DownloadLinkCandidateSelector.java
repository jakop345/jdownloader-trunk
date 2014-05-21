package jd.controlling.downloadcontroller;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import jd.controlling.downloadcontroller.AccountCache.ACCOUNTTYPE;
import jd.controlling.downloadcontroller.AccountCache.CachedAccount;
import jd.controlling.downloadcontroller.DownloadLinkCandidateResult.RESULT;
import jd.controlling.proxy.AbstractProxySelectorImpl;
import jd.controlling.proxy.ProxyController;
import jd.plugins.Account;
import jd.plugins.DownloadLink;

import org.appwork.exceptions.WTFException;

public class DownloadLinkCandidateSelector {

    private static class CandidateResultHolder {
        private final DownloadLinkCandidateResult result;

        public DownloadLinkCandidateResult getResult() {
            return result;
        }

        public DownloadLinkCandidate getCandidate() {
            return candidate;
        }

        private final DownloadLinkCandidate candidate;

        private CandidateResultHolder(DownloadLinkCandidate candidate, DownloadLinkCandidateResult result) {
            this.result = result;
            this.candidate = candidate;
        }
    }

    public static enum DownloadLinkCandidatePermission {
        OK,
        OK_FORCED,
        CONCURRENCY_LIMIT,
        CONCURRENCY_FORBIDDEN,
        IMPOSSIBLE,
        TEMP_DISABLED,
        DISABLED
    }

    private final Comparator<CandidateResultHolder>                                                        RESULT_SORTER = new Comparator<CandidateResultHolder>() {
                                                                                                                             private final DownloadLinkCandidateResult.RESULT[] FINAL_RESULT_SORT_ORDER = new RESULT[] { DownloadLinkCandidateResult.RESULT.SKIPPED, DownloadLinkCandidateResult.RESULT.ACCOUNT_REQUIRED, DownloadLinkCandidateResult.RESULT.PLUGIN_DEFECT, DownloadLinkCandidateResult.RESULT.FATAL_ERROR, DownloadLinkCandidateResult.RESULT.OFFLINE_UNTRUSTED };

                                                                                                                             private int indexOf(RESULT o1) {
                                                                                                                                 for (int index = 0; index < FINAL_RESULT_SORT_ORDER.length; index++) {
                                                                                                                                     if (FINAL_RESULT_SORT_ORDER[index] == o1) {
                                                                                                                                         return index;
                                                                                                                                     }
                                                                                                                                 }
                                                                                                                                 return -1;
                                                                                                                             }

                                                                                                                             private int compare(long x, long y) {
                                                                                                                                 return (x < y) ? -1 : ((x == y) ? 0 : 1);
                                                                                                                             }

                                                                                                                             @Override
                                                                                                                             public int compare(CandidateResultHolder o1, CandidateResultHolder o2) {
                                                                                                                                 long i1 = indexOf(o1.getResult().getResult());
                                                                                                                                 long i2 = indexOf(o2.getResult().getResult());
                                                                                                                                 if (i1 >= 0 && i2 < 0) {
                                                                                                                                     return -1;
                                                                                                                                 } else if (i2 >= 0 && i1 < 0) {
                                                                                                                                     return 1;
                                                                                                                                 } else if (i1 >= 0 && i2 >= 0) {
                                                                                                                                     return compare(i1, i2);
                                                                                                                                 } else {
                                                                                                                                     i1 = o1.getResult().getRemainingTime();
                                                                                                                                     i2 = o2.getResult().getRemainingTime();
                                                                                                                                     return -compare(i1, i2);
                                                                                                                                 }
                                                                                                                             };
                                                                                                                         };

    private final DownloadSession                                                                          session;

    private LinkedHashMap<DownloadLink, LinkedHashMap<DownloadLinkCandidate, DownloadLinkCandidateResult>> roundResults  = new LinkedHashMap<DownloadLink, LinkedHashMap<DownloadLinkCandidate, DownloadLinkCandidateResult>>();

    public DownloadSession getSession() {
        return session;
    }

    public DownloadLinkCandidateSelector(DownloadSession session) {
        this.session = session;
    }

    public int getMaxNumberOfDownloadLinkCandidatesResults(DownloadLinkCandidate candidate) {
        return -1;
    }

    public List<AbstractProxySelectorImpl> getProxies(DownloadLinkCandidate candidate) {
        return ProxyController.getInstance().getPossibleProxies(candidate);
    }

    public DownloadLinkCandidatePermission getDownloadLinkCandidatePermission(DownloadLinkCandidate candidate) {
        CachedAccount cachedAccount = candidate.getCachedAccount();
        if (cachedAccount == null) {
            return DownloadLinkCandidatePermission.IMPOSSIBLE;
        }
        if (session.isUseAccountsEnabled() == false && (cachedAccount.getType() == ACCOUNTTYPE.MULTI || cachedAccount.getType() == ACCOUNTTYPE.ORIGINAL)) {
            return DownloadLinkCandidatePermission.DISABLED;
        }
        final Account canidateAccount = cachedAccount.getAccount();
        if (canidateAccount != null) {
            if (canidateAccount.isTempDisabled()) {
                return DownloadLinkCandidatePermission.TEMP_DISABLED;
            }
            if (!canidateAccount.isEnabled()) {
                return DownloadLinkCandidatePermission.DISABLED;
            }
        }
        final DownloadLink candidateLink = candidate.getLink();
        final String linkHost = candidateLink.getHost();
        final String pluginHost = cachedAccount.getPlugin().getHost();
        int maxConcurrentAccount = cachedAccount.getPlugin().getMaxSimultanDownload(null, canidateAccount);
        int maxConcurrentHost = cachedAccount.getPlugin().getMaxSimultanDownload(candidateLink, canidateAccount);
        int concurrentHost = session.getMaxConcurrentDownloadsPerHost();
        for (SingleDownloadController con : session.getControllers()) {
            if (con.isActive()) {
                Account conAccount = con.getAccount();
                if (conAccount != null && conAccount == canidateAccount && conAccount.isConcurrentUsePossible() == false) {
                    return DownloadLinkCandidatePermission.CONCURRENCY_FORBIDDEN;
                }
                if (maxConcurrentAccount <= 0 || concurrentHost <= 0) {
                    break;
                }
                if (con.getDownloadLink().getHost().equals(linkHost)) {
                    concurrentHost--;
                }
                if (canidateAccount != null && canidateAccount == conAccount) {
                    maxConcurrentAccount--;
                    if (con.getDownloadLink().getHost().equals(linkHost)) {
                        maxConcurrentHost--;
                    }
                } else if (conAccount == null && pluginHost.equals(con.getDownloadLinkCandidate().getCachedAccount().getPlugin().getHost())) {
                    maxConcurrentAccount--;
                    if (con.getDownloadLink().getHost().equals(linkHost)) {
                        maxConcurrentHost--;
                    }
                }
            }
        }
        if (concurrentHost > 0 && maxConcurrentAccount > 0 && maxConcurrentHost > 0) {
            return DownloadLinkCandidatePermission.OK;
        }
        if (candidate.isForced()) {
            return DownloadLinkCandidatePermission.OK_FORCED;
        }
        return DownloadLinkCandidatePermission.CONCURRENCY_LIMIT;
    }

    public boolean isMirrorManagement() {
        return session.isMirrorManagementEnabled();
    }

    public boolean isForcedOnly() {
        return forcedOnly;
    }

    public void setForcedOnly(boolean forcedOnly) {
        this.forcedOnly = forcedOnly;
    }

    public boolean isExcluded(DownloadLink link) {
        return roundResults.containsKey(link);
    }

    public void addExcluded(DownloadLink link) {
        if (roundResults.containsKey(link)) {
            return;
        }
        roundResults.put(link, null);
    }

    public boolean validateDownloadLinkCandidate(DownloadLinkCandidate possibleCandidate) {
        DownloadLinkCandidateResult linkResult = null;
        DownloadLinkCandidateResult proxyResult = null;
        DownloadLinkCandidateHistory linkHistory = getSession().getHistory(possibleCandidate.getLink());
        if (linkHistory != null) {
            linkResult = linkHistory.getBlockingHistory(this, possibleCandidate);
        }
        ProxyInfoHistory proxyHistory = getSession().getProxyInfoHistory();
        proxyResult = proxyHistory.getBlockingHistory(possibleCandidate);
        if (linkResult != null && proxyResult == null) {
            addExcluded(possibleCandidate, linkResult);
            return false;
        } else if (proxyResult != null && linkResult == null) {
            addExcluded(possibleCandidate, proxyResult);
            return false;
        } else if (proxyResult != null && linkResult != null) {
            switch (linkResult.getResult()) {
            case PLUGIN_DEFECT:
            case OFFLINE_UNTRUSTED:
            case ACCOUNT_REQUIRED:
            case FATAL_ERROR:
            case SKIPPED:
                addExcluded(possibleCandidate, linkResult);
                break;
            case FILE_UNAVAILABLE:
            case CONNECTION_ISSUES:
                if (proxyResult.getRemainingTime() >= linkResult.getRemainingTime()) {
                    addExcluded(possibleCandidate, proxyResult);
                } else {
                    addExcluded(possibleCandidate, linkResult);
                }
                break;
            default:
                System.out.println("FIXME " + linkResult.getResult());
                break;
            }
            return false;
        }
        return true;
    }

    public void setExcluded(DownloadLink link) {
        roundResults.put(link, null);
    }

    public void addExcluded(DownloadLinkCandidate candidate, DownloadLinkCandidateResult result) {
        if (result == null) {
            throw new IllegalArgumentException("result == null");
        }
        LinkedHashMap<DownloadLinkCandidate, DownloadLinkCandidateResult> map = roundResults.get(candidate.getLink());
        if (map == null) {
            map = new LinkedHashMap<DownloadLinkCandidate, DownloadLinkCandidateResult>();
            roundResults.put(candidate.getLink(), map);
        }
        map.put(candidate, result);
    }

    public LinkedHashMap<DownloadLinkCandidate, DownloadLinkCandidateResult> finalizeDownloadLinkCandidatesResults() {
        LinkedHashMap<DownloadLinkCandidate, DownloadLinkCandidateResult> ret = new LinkedHashMap<DownloadLinkCandidate, DownloadLinkCandidateResult>();
        Iterator<Entry<DownloadLink, LinkedHashMap<DownloadLinkCandidate, DownloadLinkCandidateResult>>> it = roundResults.entrySet().iterator();
        linkLoop: while (it.hasNext()) {
            Entry<DownloadLink, LinkedHashMap<DownloadLinkCandidate, DownloadLinkCandidateResult>> next = it.next();
            Map<DownloadLinkCandidate, DownloadLinkCandidateResult> map = next.getValue();
            if (map == null || map.size() == 0) {
                continue;
            }
            List<CandidateResultHolder> results = new ArrayList<DownloadLinkCandidateSelector.CandidateResultHolder>();
            Iterator<Entry<DownloadLinkCandidate, DownloadLinkCandidateResult>> it2 = map.entrySet().iterator();
            while (it2.hasNext()) {
                Entry<DownloadLinkCandidate, DownloadLinkCandidateResult> next2 = it2.next();
                DownloadLinkCandidateResult candidateResult = next2.getValue();
                switch (candidateResult.getResult()) {
                case CONNECTION_UNAVAILABLE:
                    continue linkLoop;
                case PLUGIN_DEFECT:
                case OFFLINE_UNTRUSTED:
                case ACCOUNT_REQUIRED:
                case FATAL_ERROR:
                case SKIPPED:
                case PROXY_UNAVAILABLE:
                case FILE_UNAVAILABLE:
                case CONNECTION_ISSUES:
                case CONDITIONAL_SKIPPED:
                    results.add(new CandidateResultHolder(next2.getKey(), candidateResult));
                    break;
                default:
                    throw new WTFException("This should not happen " + candidateResult.getResult());
                }
                Collections.sort(results, RESULT_SORTER);
                CandidateResultHolder mostImportantResult = results.get(0);
                ret.put(mostImportantResult.getCandidate(), mostImportantResult.getResult());
            }
        }
        return ret;
    }

    private boolean forcedOnly = false;

}
