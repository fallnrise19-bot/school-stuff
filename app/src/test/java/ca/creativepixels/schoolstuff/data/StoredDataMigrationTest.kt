package ca.creativepixels.schoolstuff.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class StoredDataMigrationTest {
    @Test
    fun oldChildWithoutTransportationFieldsGetsSafeDefaults() {
        val oldJson = """[{"id":"child-1","name":"Oliver","grade":"5","colorKey":"blue"}]"""

        val child = StoredDataMigration.children(oldJson)!!.single()

        assertEquals("Oliver", child.name)
        assertEquals("", child.transportationType)
        assertEquals("", child.busNumber)
        assertEquals("", child.transportationNotes)
    }

    @Test
    fun explicitNullsInSavedModelsAreRepaired() {
        val childJson = """[{"id":"child-2","name":"Chloe","pickupInfo":null,"specialNotes":null}]"""
        val itemJson = """[{"id":"item-1","title":null,"category":null,"dateIso":null}]"""
        val documentJson = """[{"id":"doc-1","childId":"child-2","title":null,"type":null,"uri":null}]"""

        val child = StoredDataMigration.children(childJson)!!.single()
        val item = StoredDataMigration.items(itemJson)!!.single()
        val document = StoredDataMigration.documents(documentJson)!!.single()

        assertEquals("", child.pickupInfo)
        assertEquals("", child.specialNotes)
        assertEquals("School item", item.title)
        assertNotNull(item.dateIso)
        assertEquals("School file", document.title)
        assertEquals("", document.uri)
    }
}
