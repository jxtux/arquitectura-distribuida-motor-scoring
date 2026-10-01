package com.finanscore.scoring.application;

import java.math.BigDecimal;

public interface CreditDataProviderPort {
	CreditProfile get(Long userId);

	record CreditProfile(Long userId, String documentNumber, String displayName, BigDecimal monthlyIncome,
			BigDecimal monthlyExpenses, BigDecimal monthlyObligations, int employmentMonths, int activeObligations,
			int paymentHistoryScore, int delinquencyAlerts) {
	}
}