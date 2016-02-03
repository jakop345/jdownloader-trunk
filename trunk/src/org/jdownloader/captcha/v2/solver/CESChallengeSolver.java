package org.jdownloader.captcha.v2.solver;

import jd.SecondLevelLaunch;
import jd.gui.swing.jdgui.components.premiumbar.ServicePanel;

import org.appwork.storage.config.ValidationException;
import org.appwork.storage.config.events.GenericConfigEventListener;
import org.appwork.storage.config.handler.KeyHandler;
import org.appwork.utils.Application;
import org.jdownloader.captcha.v2.Challenge;
import org.jdownloader.captcha.v2.ChallengeSolver;
import org.jdownloader.captcha.v2.SolverService;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.RecaptchaV2Challenge;
import org.jdownloader.captcha.v2.challenge.stringcaptcha.BasicCaptchaChallenge;
import org.jdownloader.captcha.v2.solver.jac.SolverException;
import org.jdownloader.captcha.v2.solverjob.SolverJob;

public abstract class CESChallengeSolver<T> extends ChallengeSolver<T> {
    protected int getDefaultWaitForOthersTimeout() {
        return 60000;
    }

    public CESChallengeSolver(int threadCount) {
        super(null, threadCount);

    }

    public CESChallengeSolver(SolverService service, int threadCount) {
        super(service, threadCount);

    }

    public boolean canHandle(Challenge<?> c) {
        return c.isAccountLogin() == false && super.canHandle(c);
    }

    final public void solve(final SolverJob<T> job) throws InterruptedException, SolverException {
        if (!validateLogins()) {
            return;
        }
        if (!isEnabled() || !canHandle(job.getChallenge())) {
            return;
        }

        checkInterruption();
        CESSolverJob<T> cesJob = new CESSolverJob<T>(job);
        try {
            solveCES(cesJob);
        } finally {
            cesJob.hideBubble();
        }
    }

    protected void solveCES(CESSolverJob<T> job) throws InterruptedException, SolverException {
        Challenge<?> challenge = job.getChallenge();
        if (challenge instanceof RecaptchaV2Challenge) {
            challenge = ((RecaptchaV2Challenge) challenge).createBasicCaptchaChallenge();

        }

        solveBasicCaptchaChallenge(job, (BasicCaptchaChallenge) challenge);

    }

    protected abstract void solveBasicCaptchaChallenge(CESSolverJob<T> job, BasicCaptchaChallenge challenge) throws InterruptedException, SolverException;

    protected abstract boolean validateLogins();

    protected void initServicePanel(final KeyHandler... handlers) {

        SecondLevelLaunch.GUI_COMPLETE.executeWhenReached(new Runnable() {

            @SuppressWarnings("unchecked")
            public void run() {

                for (KeyHandler k : handlers) {

                    k.getEventSender().addListener(new GenericConfigEventListener<Object>() {

                        @Override
                        public void onConfigValidatorError(KeyHandler<Object> keyHandler, Object invalidValue, ValidationException validateException) {
                        }

                        @Override
                        public void onConfigValueModified(KeyHandler<Object> keyHandler, Object newValue) {
                            if (!Application.isHeadless()) {
                                ServicePanel.getInstance().requestUpdate(true);
                            }
                        }
                    });
                }

            }
        });

    }

    public String getAccountStatusString() {
        return null;
    }
}