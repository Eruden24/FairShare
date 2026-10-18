package io.github.eruden24.fairshare.repository;

import java.util.UUID;

/** A sum of minor units per group, from a "group by group" query. */
public interface GroupTotal {
    UUID getGroupId();
    long getTotal();
}
