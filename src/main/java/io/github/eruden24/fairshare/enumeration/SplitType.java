package io.github.eruden24.fairshare.enumeration;

/**
 * How an expense's amount is divided between its participants.
 */
public enum SplitType {
    /** Same share for everyone. Participants give no value. */
    EQUAL,
    /** Each participant's value is their exact amount; the values must add up to the expense amount. */
    EXACT,
    /** Each participant's value is a percentage; the values must add up to 100. */
    PERCENTAGE,
    /** Each participant's value is a number of shares, e.g. 2 for someone who stayed two nights. */
    SHARES
}
