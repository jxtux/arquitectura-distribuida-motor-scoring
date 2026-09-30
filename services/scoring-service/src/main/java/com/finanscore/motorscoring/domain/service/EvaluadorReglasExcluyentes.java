package com.finanscore.motorscoring.domain.service;

import com.finanscore.motorscoring.domain.entity.ResultadoFactor;
import com.finanscore.motorscoring.domain.enums.ResultadoScoring;
import java.util.*;

public final class EvaluadorReglasExcluyentes {
	
	public Optional<ResultadoScoring> evaluar(List<ResultadoFactor> r) {
		
		return r.stream().filter(ResultadoFactor::reglaExcluyente).map(ResultadoFactor::resultadoExcluyente).findFirst();
	}
}
