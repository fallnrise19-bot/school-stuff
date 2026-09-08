package ca.creativepixels.schoolstuff.data

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class SchoolYearTest {
    @Test
    fun septemberStartsNewSchoolYear() {
        val date = LocalDate.of(2026, 9, 8)

        assertEquals(LocalDate.of(2026, 9, 1), schoolYearStartFor(date))
        assertEquals(LocalDate.of(2027, 8, 31), schoolYearEndFor(date))
    }

    @Test
    fun summerBelongsToPreviousSchoolYear() {
        val date = LocalDate.of(2027, 7, 15)

        assertEquals(LocalDate.of(2026, 9, 1), schoolYearStartFor(date))
        assertEquals(LocalDate.of(2027, 8, 31), schoolYearEndFor(date))
    }

    @Test
    fun augustBoundaryEndsSchoolYear() {
        val date = LocalDate.of(2027, 8, 31)

        assertEquals(LocalDate.of(2026, 9, 1), schoolYearStartFor(date))
        assertEquals(LocalDate.of(2027, 8, 31), schoolYearEndFor(date))
    }
}
