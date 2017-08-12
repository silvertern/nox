/**
 * Created by skol on 02.03.17.
 */
package nox.core

import org.junit.Assert.assertEquals
import org.junit.Test


class VersionTest {

	@Test
	fun version_maxGtMin_success() {
		assertEquals(Version.MIN.compareTo(Version.MAX).toLong(), -1)
	}

	@Test
	fun version_min_correct() {
		assertEquals(listOf(Version.MAX, Version.DEFAULT).min(), Version.DEFAULT)
		assertEquals(listOf(Version.DEFAULT).min(), Version.DEFAULT)
	}

	@Test
	fun version_max_correct() {
		assertEquals(listOf(Version.DEFAULT, Version.MIN).max(), Version.DEFAULT)
		assertEquals(listOf(Version.DEFAULT, Version.MIN).max(), Version.DEFAULT)
	}

	@Test
	fun version_parseFromString_withDashInBuild_correct() {
		assertEquals("5.4.2.SNAPSHOT", Version("5.4.2-SNAPSHOT").toString())
		assertEquals("5.4.2.12356-5678", Version("5.4.2_12356_5678").toString())
	}

}
