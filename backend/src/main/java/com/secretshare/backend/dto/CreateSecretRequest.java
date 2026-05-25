package com.secretshare.backend.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CreateSecretRequest {

    @NotBlank
    @Size(max = 10000)
    private String value;

    @Min(1)
    @Max(100)
    private Integer maxUses;

    @Min(1)
    @Max(8760)
    private Integer ttlHours;

    public CreateSecretRequest() {}

    public @NotBlank @Size(max = 10000) String getValue() { return value; }
    public void setValue(@NotBlank @Size(max = 10000) String value) { this.value = value; }

    public @Min(1) @Max(100) Integer getMaxUses() { return maxUses; }
    public void setMaxUses(@Min(1) @Max(100) Integer maxUses) { this.maxUses = maxUses; }

    public @Min(1) @Max(8760) Integer getTtlHours() { return ttlHours; }
    public void setTtlHours(@Min(1) @Max(8760) Integer ttlHours) { this.ttlHours = ttlHours; }
}
