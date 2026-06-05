package com.dressme.dressme_database.model;

import java.io.Serializable;
import java.util.UUID;

public class DressCodeCompatibilityId implements Serializable {
    private UUID dressCodeId;
    private UUID compatibleWithId;

    public DressCodeCompatibilityId() {}

    public DressCodeCompatibilityId(UUID dressCodeId, UUID compatibleWithId) {
        this.dressCodeId = dressCodeId;
        this.compatibleWithId = compatibleWithId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DressCodeCompatibilityId other)) return false;
        return java.util.Objects.equals(dressCodeId, other.dressCodeId)
            && java.util.Objects.equals(compatibleWithId, other.compatibleWithId);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(dressCodeId, compatibleWithId);
    }
}
