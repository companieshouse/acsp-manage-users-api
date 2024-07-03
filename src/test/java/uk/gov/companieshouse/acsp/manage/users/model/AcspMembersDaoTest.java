package uk.gov.companieshouse.acsp.manage.users.model;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.Test;

public class AcspMembersDaoTest {

  @Test
  public void testHasBeenRemoved() {
    AcspMembersDao dao = new AcspMembersDao();
    assertFalse(dao.beenRemoved());

    dao.setRemovedBy("user123");
    assertTrue(dao.beenRemoved());
  }
}
