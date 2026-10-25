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
}
