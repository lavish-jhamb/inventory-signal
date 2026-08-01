plugins {
	java
	id("org.springframework.boot") version "4.1.0"
	id("io.spring.dependency-management") version "1.1.7"
}

group = "com.notify.inventory"
version = "0.0.1-SNAPSHOT"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(25)
	}
}

repositories {
	mavenCentral()
}

dependencies {
	implementation("org.springframework.boot:spring-boot-starter-webmvc")

	implementation("org.apache.httpcomponents.client5:httpclient5")
	implementation("org.jsoup:jsoup:1.22.2")

	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test> {
	useJUnitPlatform()
}

// Run:  ./gradlew runDumper
tasks.register<JavaExec>("runDumper") {
	group = "diagnostic"
	description = "Fetches both Croma PDP pages, saves HTML to samples/, and prints a comparison."
	classpath = sourceSets["main"].runtimeClasspath
	mainClass.set("com.notify.inventory.signal.debug.HtmlSampleDumper")
	workingDir = rootProject.projectDir
}

// Run:  ./gradlew runValidator
tasks.register<JavaExec>("runValidator") {
	group = "diagnostic"
	description = "Replays determineStockStatus() against saved samples/*.html and checks known outcomes."
	classpath = sourceSets["main"].runtimeClasspath
	mainClass.set("com.notify.inventory.signal.http.StockSignalValidator")
	workingDir = rootProject.projectDir
}

// Run:  ./gradlew runMobileScan
tasks.register<JavaExec>("runMobileScan") {
	group = "diagnostic"
	description = "Fetches ~20 real Croma mobile phone PDPs and reports stock status for each."
	classpath = sourceSets["main"].runtimeClasspath
	mainClass.set("com.notify.inventory.signal.http.MobilePhoneStockScanner")
	workingDir = rootProject.projectDir
}

// Run:  ./gradlew runPincodeCheck
tasks.register<JavaExec>("runPincodeCheck") {
	group = "diagnostic"
	description = "Checks a product's delivery availability across multiple pincodes via Croma's inventory API."
	classpath = sourceSets["main"].runtimeClasspath
	mainClass.set("com.notify.inventory.signal.http.PincodeAwareStockChecker")
	workingDir = rootProject.projectDir
}

