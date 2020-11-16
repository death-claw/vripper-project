package tn.mnlr.vripper.services.domain;

import lombok.Getter;

@Getter
public class DownloadSpeed {

    private final String speed;

    public DownloadSpeed(long bytes) {
        speed = formatSI(bytes);
    }

    private String formatSI(long bytes) {
        return humanReadableByteCount(bytes, false);
    }

    private String humanReadableByteCount(long bytes, boolean si) {
        int unit = si ? 1000 : 1024;
        if (bytes < unit) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(unit));
        String pre = (si ? "kMGTPE" : "KMGTPE").charAt(exp - 1) + (si ? "" : "i");
        return String.format("%.1f %sB", bytes / Math.pow(unit, exp), pre);
    }
}
