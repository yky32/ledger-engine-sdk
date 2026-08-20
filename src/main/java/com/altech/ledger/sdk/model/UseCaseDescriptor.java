package com.altech.ledger.sdk.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * One ops-configured use case from {@code GET /integrations/use-cases}.
 * Upstream picks {@link #code} and submits via {@link com.altech.ledger.sdk.api.UseCaseApi#invoke}.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public final class UseCaseDescriptor {
    private String code;
    private String name;
    private Boolean enabled;
    private String operation;
    private Integer priority;
    private String pointCurrency;
    /** ZERO | SPEND | ANY */
    private String amountMode;
    private Object formula;
    private String formulaSummary;
    private String coaProfileCode;
    private String coaCurrency;
    private Boolean hasBrainRule;
    private Boolean hasCoaProfile;
    private Boolean hasRecipe;

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }
    public String getOperation() { return operation; }
    public void setOperation(String operation) { this.operation = operation; }
    public Integer getPriority() { return priority; }
    public void setPriority(Integer priority) { this.priority = priority; }
    public String getPointCurrency() { return pointCurrency; }
    public void setPointCurrency(String pointCurrency) { this.pointCurrency = pointCurrency; }
    public String getAmountMode() { return amountMode; }
    public void setAmountMode(String amountMode) { this.amountMode = amountMode; }
    public Object getFormula() { return formula; }
    public void setFormula(Object formula) { this.formula = formula; }
    public String getFormulaSummary() { return formulaSummary; }
    public void setFormulaSummary(String formulaSummary) { this.formulaSummary = formulaSummary; }
    public String getCoaProfileCode() { return coaProfileCode; }
    public void setCoaProfileCode(String coaProfileCode) { this.coaProfileCode = coaProfileCode; }
    public String getCoaCurrency() { return coaCurrency; }
    public void setCoaCurrency(String coaCurrency) { this.coaCurrency = coaCurrency; }
    public Boolean getHasBrainRule() { return hasBrainRule; }
    public void setHasBrainRule(Boolean hasBrainRule) { this.hasBrainRule = hasBrainRule; }
    public Boolean getHasCoaProfile() { return hasCoaProfile; }
    public void setHasCoaProfile(Boolean hasCoaProfile) { this.hasCoaProfile = hasCoaProfile; }
    public Boolean getHasRecipe() { return hasRecipe; }
    public void setHasRecipe(Boolean hasRecipe) { this.hasRecipe = hasRecipe; }

    public boolean isSpendAmountRequired() {
        return "SPEND".equalsIgnoreCase(amountMode);
    }

    public boolean isZeroAmountOk() {
        return "ZERO".equalsIgnoreCase(amountMode) || "ANY".equalsIgnoreCase(amountMode);
    }

    @Override
    public String toString() {
        return "UseCaseDescriptor{code='" + code + "', name='" + name
            + "', amountMode=" + amountMode + ", formula=" + formulaSummary
            + ", coa=" + coaProfileCode + "}";
    }
}
