package com.clicclap;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

class ModularityTests {

	ApplicationModules modules = ApplicationModules.of(ClicclapApplication.class);

	@Test
	void verifiesModularStructure() {
		modules.forEach(System.out::println);
		modules.verify();
	}

}
