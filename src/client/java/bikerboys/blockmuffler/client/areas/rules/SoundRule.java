package bikerboys.blockmuffler.client.areas.rules;

public class SoundRule {
    public String soundId;
    public float volumeMul = 1.0f;
    public boolean blocked = false;

    public SoundRule(String soundId) {
        this.soundId = soundId;
    }

    public boolean isBlocked() {
        return blocked || volumeMul == 0.0f;
    }

    public float getVolumeMul() {
        return volumeMul;
    }

    public void setVolumeMul(float volumeMul) {
        this.volumeMul = volumeMul;
    }

    public void setBlocked(boolean blocked) {
        this.blocked = blocked;
    }

    public void setSoundId(String soundId) {
        this.soundId = soundId;
    }

    public boolean isWildcard() {
        return soundId.indexOf('*') >= 0;
    }

    public boolean matches(String id) {
        return matches(soundId, id);
    }

    public int specificity() {
        int literals = 0;
        for (int i = 0; i < soundId.length(); i++) {
            if (soundId.charAt(i) != '*') {
                literals++;
            }
        }
        return literals * 2 + (isWildcard() ? 0 : 1);
    }

    public static boolean matches(String pattern, String id) {
        if (pattern.indexOf('*') < 0) {
            return pattern.equals(id);
        }

        int p = 0;
        int t = 0;
        int star = -1;
        int mark = 0;

        while (t < id.length()) {
            if (p < pattern.length() && pattern.charAt(p) == '*') {
                star = p++;
                mark = t;
            } else if (p < pattern.length() && pattern.charAt(p) == id.charAt(t)) {
                p++;
                t++;
            } else if (star >= 0) {
                p = star + 1;
                t = ++mark;
            } else {
                return false;
            }
        }

        while (p < pattern.length() && pattern.charAt(p) == '*') {
            p++;
        }
        return p == pattern.length();
    }
}