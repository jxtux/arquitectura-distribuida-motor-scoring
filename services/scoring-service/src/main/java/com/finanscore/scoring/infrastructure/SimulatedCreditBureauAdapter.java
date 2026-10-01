package com.finanscore.scoring.infrastructure;

import com.finanscore.scoring.application.CreditDataProviderPort;
import org.springframework.stereotype.Component;

@Component
public class SimulatedCreditBureauAdapter implements CreditDataProviderPort {
	private static final long SYNTHETIC_PROFILE_COUNT = 5L;
	private final SimulatedCreditProfileRepository repo;

	public SimulatedCreditBureauAdapter(SimulatedCreditProfileRepository r) {
		repo = r;
	}

	@Override
	public CreditProfile get(Long userId) {
		var exact = repo.findById(userId);
		if (exact.isPresent()) {
			var p = exact.get();
			return new CreditProfile(p.userId, p.documentNumber, p.displayName, p.monthlyIncome, p.monthlyExpenses,
					p.monthlyObligations, p.employmentMonths, p.activeObligations, p.paymentHistoryScore,
					p.delinquencyAlerts);
		}

		long fallbackId = ((Math.max(userId, 1L) - 1L) % SYNTHETIC_PROFILE_COUNT) + 1L;
		var synthetic = repo.findById(fallbackId).orElseThrow(
				() -> new IllegalStateException("No existe perfil crediticio simulado para userId=" + userId));

		return new CreditProfile(userId, synthetic.documentNumber, synthetic.displayName, synthetic.monthlyIncome,
				synthetic.monthlyExpenses, synthetic.monthlyObligations, synthetic.employmentMonths,
				synthetic.activeObligations, synthetic.paymentHistoryScore, synthetic.delinquencyAlerts);
	}
}
