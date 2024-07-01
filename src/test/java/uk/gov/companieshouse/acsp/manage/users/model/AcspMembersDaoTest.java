package uk.gov.companieshouse.acsp.manage.users.model;

import org.junit.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AcspMembersDaoTest {

    @Test
    public void testHasBeenRemoved() {
        AcspMembersDao dao = new AcspMembersDao();
        assertFalse(dao.hasBeenRemoved());

        dao.setRemovedBy("user123");
        assertTrue(dao.hasBeenRemoved());
    }

}