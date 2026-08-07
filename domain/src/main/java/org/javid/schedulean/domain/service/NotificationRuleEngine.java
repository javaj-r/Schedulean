package org.javid.schedulean.domain.service;

import org.javid.schedulean.domain.model.JobFailureEvent;
import org.javid.schedulean.domain.valueobject.enums.NotificationChannel;

import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;

public class NotificationRuleEngine {

    private final List<NotificationRule> rules;

    public NotificationRuleEngine(List<NotificationRule> rules) {
        Objects.requireNonNull(rules, "rules cannot be null");
        // Defensive copy of mutable list
        this.rules = List.copyOf(rules);
    }

    public Set<NotificationChannel> resolve(JobFailureEvent event) {
        Objects.requireNonNull(event, "event cannot be null");
        Set<NotificationChannel> channels = EnumSet.noneOf(NotificationChannel.class);
        for (NotificationRule rule : rules) {
            if (rule.matches(event)) {
                channels.addAll(rule.channels());
            }
        }
        return channels;
    }

    public record NotificationRule(Predicate<JobFailureEvent> condition, Set<NotificationChannel> channels) {
        public NotificationRule {
            Objects.requireNonNull(condition, "condition cannot be null");
            Objects.requireNonNull(channels, "channels cannot be null");
            // Defensive copy of mutable set
            channels = Set.copyOf(channels);
        }

        public boolean matches(JobFailureEvent e) {
            return condition.test(e);
        }
    }
}
