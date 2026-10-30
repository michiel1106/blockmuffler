package bikerboys.blockmuffler.client.areas.rules;

import net.minecraft.sounds.SoundSource;

import java.util.*;

public class AreaRule {
    float volMultiplier = 1.0f;
    boolean includeEntities = true;
    List<SoundRule> soundRules = new ArrayList<>();
    Map<SoundSource, Float> categoryMultipliers = new EnumMap<>(SoundSource.class);


    public float getVolMultiplier() {
        return volMultiplier;
    }

    public void setVolMultiplier(float volMultiplier) {
        this.volMultiplier = Math.max(0.0f, Math.min(1.0f, volMultiplier));
    }

    public boolean isIncludeEntities() {
        return includeEntities;
    }

    public void setIncludeEntities(boolean includeEntities) {
        this.includeEntities = includeEntities;
    }

    public List<SoundRule> getSoundRules() {
        return Collections.unmodifiableList(soundRules);
    }


    public float getCategoryMultiplier(SoundSource category) {
        return categoryMultipliers.getOrDefault(category, 1.0f);
    }

    public void setCategoryMultiplier(SoundSource category, float multiplier) {
        float clamped = Math.max(0.0f, Math.min(1.0f, multiplier));
        if (clamped >= 1.0f) {
            categoryMultipliers.remove(category);
        } else {
            categoryMultipliers.put(category, clamped);
        }
    }

    public void resetCategoryMultiplier(SoundSource category) {
        categoryMultipliers.remove(category);
    }

    public Map<SoundSource, Float> getCategoryMultipliers() {
        return Collections.unmodifiableMap(categoryMultipliers);
    }

    public SoundRule getSoundRule(String soundId) {
        for (SoundRule rule : soundRules) {
            if (rule.soundId.equals(soundId)) {
                return rule;
            }
        }
        return null;
    }

    public SoundRule findMatchingRule(String soundId) {
        SoundRule best = null;
        int bestSpecificity = -1;

        for (SoundRule rule : soundRules) {
            if (!rule.matches(soundId)) continue;

            int specificity = rule.specificity();
            if (specificity > bestSpecificity) {
                best = rule;
                bestSpecificity = specificity;
            }
        }
        return best;
    }

    public float getVolumeMultiplier(String soundId, SoundSource category) {
        float result = volMultiplier * getCategoryMultiplier(category);

        SoundRule rule = findMatchingRule(soundId);
        if (rule != null) {
            if (rule.isBlocked()) return 0.0f;
            result *= rule.getVolumeMul();
        }

        return result;
    }

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