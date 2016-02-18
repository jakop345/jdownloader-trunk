package jd.plugins.download;

import org.appwork.utils.StringUtils;

public class HashInfo {

    public static enum TYPE {
        MD5("MD5", 32),
        CRC32("CRC32", 8),
        SHA1("SHA1", 40),
        SHA256("SHA-256", 64);

        private final String digest;
        private final int    size;

        public final String getDigest() {
            return digest;
        }

        private TYPE(final String digest, int size) {
            this.digest = digest;
            this.size = size;
        }

        public final int getSize() {
            return size;
        }
    }

    private final String hash;

    public String getHash() {
        return hash;
    }

    public TYPE getType() {
        return type;
    }

    private final TYPE    type;
    private final boolean trustworthy;

    public boolean isTrustworthy() {
        return trustworthy;
    }

    public String exportAsString() {
        return getType() + "|" + (isTrustworthy() ? "1" : "0") + "|" + getHash();
    }

    public static HashInfo importFromString(final String hashInfo) {
        if (hashInfo != null) {
            try {
                final String parts[] = hashInfo.split("\\|");
                if (parts != null && parts.length == 3) {
                    final TYPE type = TYPE.valueOf(parts[0]);
                    final boolean trustworthy = "1".equals(parts[1]);
                    return new HashInfo(parts[2], type, trustworthy);
                }
            } catch (final Throwable e) {
            }
        }
        return null;
    }

    /**
     * @param hash
     * @param type
     */
    public HashInfo(String hash, TYPE type, boolean trustworthy) {
        if (StringUtils.isEmpty(hash)) {
            throw new IllegalArgumentException("hash is empty");
        }
        if (type == null) {
            throw new IllegalArgumentException("type is missing");
        }
        this.type = type;
        if (hash.length() < type.getSize()) {
            hash = String.format("%0" + (type.getSize() - hash.length()) + "d%s", 0, hash);
        } else if (hash.length() > type.getSize()) {
            hash = hash.substring(0, type.getSize());
        }
        this.hash = hash;
        this.trustworthy = trustworthy;
    }

    public HashInfo(String hash, TYPE type) {
        this(hash, type, true);
    }

    @Override
    public String toString() {
        return "HashInfo:TYPE:" + type + "|Hash:" + hash + "|Trustworthy:" + trustworthy;
    }
}