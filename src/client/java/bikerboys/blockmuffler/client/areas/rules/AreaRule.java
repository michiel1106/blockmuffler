package bikerboys.blockmuffler.client.areas.rules;

import java.util.*;

public class AreaRule {
    float volMultiplier;
    boolean includeEntities;
    List<SoundRule> soundRules = new ArrayList<>();


    public boolean addSoundRule(SoundRule soundRule) {
        boolean exists = false;
        for (SoundRule rule : soundRules) {
            if (rule.soundId.equals(soundRule.soundId)) {
                exists = true;
            }
        }

        if (exists) {
            return false;
        } else {
            return soundRules.add(soundRule);
        }

    }

    public boolean removeSoundRule(SoundRule soundRule) {
        return removeSoundRule(soundRule.soundId);
    }

    public boolean removeSoundRule(String soundId) {
        return soundRules.removeIf(ruleOfSound -> ruleOfSound.soundId.equals(soundId));
    }

}
