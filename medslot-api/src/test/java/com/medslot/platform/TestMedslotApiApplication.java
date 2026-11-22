package com.medslot.platform;

import org.springframework.boot.SpringApplication;

public class TestMedslotApiApplication {

	public static void main(String[] args) {
		SpringApplication.from(MedslotApiApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
