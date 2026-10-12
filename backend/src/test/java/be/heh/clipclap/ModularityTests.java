package be.heh.clipclap;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

class ModularityTests {

	ApplicationModules modules = ApplicationModules.of(ClipclapApplication.class);

	@Test
	void verifiesModularStructure() {
		modules.forEach(System.out::println);
		modules.verify();
	}

}
