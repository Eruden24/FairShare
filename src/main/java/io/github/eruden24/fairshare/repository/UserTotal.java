package io.github.eruden24.fairshare.repository;

import java.util.UUID;

/** A sum of minor units per user, from a "group by user" query. */
public interface UserTotal {
    UUID getUserId();
    long getTotal();
}
