package com.radion.domain.enums;

/**
 * Represents high-level semantic Business Commands (World Events) emitted by the LLM
 * when reasoning over evidence (e.g. emails, classroom posts) in the student's evolving world state.
 *
 * Notice there is NO 'IGNORE' command, as irrelevant evidence simply emits an empty list of commands.
 * Notice there is NO standalone 'DEADLINE' or 'TIMELINE' entity; due dates and timeline stages are properties
 * of Opportunities, Action Items, and Assessments.
 */
public enum BusinessCommandType {
    REGISTER_OPPORTUNITY,
    ADVANCE_OPPORTUNITY_STAGE,
    SCHEDULE_INTERVIEW,
    SCHEDULE_ASSESSMENT,
    ASSIGN_ACTION_ITEM,
    COMPLETE_ACTION_ITEM,
    ANNOUNCE_EVENT
}
