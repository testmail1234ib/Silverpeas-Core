package com.silverpeas.socialNetwork.status;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import org.dbunit.database.IDatabaseConnection;
import org.junit.Test;

import com.silverpeas.components.model.AbstractTestDao;

public class TestSatusDao extends AbstractTestDao {

  private StatusDao dao;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    dao = new com.silverpeas.socialNetwork.status.StatusDao();
  }

  @Test
  public void testChangeStatus() throws Exception {
    IDatabaseConnection connexion = null;
    Status status = new Status(1, new Date(), "je teste");

    try {
      connexion = getConnection();
      int id = dao.changeStatus(connexion.getConnection(), status);
      assertNotNull("Status should have been created", id);
      assertTrue("New id is correct", id > 0);
      status.setId(id);
      Status nouveauStatus = dao.getStatus(connexion.getConnection(), id);
      assertNotNull("Status not found in db", nouveauStatus);
      assertEquals("Status in db not as expected", status, nouveauStatus);
    } finally {
      closeConnection(connexion);
    }

  }

  @Test
  public void testGetStatus() throws Exception {
    IDatabaseConnection connexion = null;
    Status status = new Status(1, toDate(2010, Calendar.FEBRUARY, 01, 10, 34, 15), "je suis là");
    int id = 1;
    status.setId(id);
    try {
      connexion = getConnection();
      Status dbStatus = dao.getStatus(connexion.getConnection(), id);
      assertNotNull("Status not found in db", dbStatus);
      assertEquals("Status in db not as expected", status, dbStatus);

    } finally {
      closeConnection(connexion);
    }
  }
  
  @Test
  public void testGetLastStatus() throws Exception {
    IDatabaseConnection connexion = null;
    Status status = new Status(1, toDate(2010, Calendar.JULY, 02, 10, 33, 10), "travaille sur readmine");
    status.setId(4);
    
    int userid = 1;
    
    try {
      connexion = getConnection();
      Status lastStatus = dao.getLastStatus(connexion.getConnection(), userid);
      assertNotNull("Status not found in db", lastStatus);
      assertEquals(status.getId(),lastStatus.getId());
      assertEquals("Status in db not as expected", status, lastStatus);

    } finally {
      closeConnection(connexion);
    }
    
  }

  @Test
  public void testDeleteStatus() throws Exception {
    IDatabaseConnection connexion = null;
    Status expectedStatus = new Status(2, toDate(2010, Calendar.MAY, 11, 15, 25, 32), "congé");
    expectedStatus.setId(2);
    try {
      connexion = getConnection();
      Status status = dao.getStatus(connexion.getConnection(), 2);
      assertNotNull("Status should exist", status);
      dao.deleteStatus(connexion.getConnection(), 2);
      status = dao.getStatus(connexion.getConnection(), 2);
      assertNull("Status should no longer exist", status);
    } finally {
      closeConnection(connexion);
    }

  }

  @Test
  public void testUpdateStatus() throws Exception {
    IDatabaseConnection connexion = null;
    Status updateStatus = new Status(2, toDate(2010, Calendar.MAY, 11, 15, 25, 32), "malade");
    updateStatus.setId(3);
    try {
      connexion = getConnection();
      Status status = dao.getStatus(connexion.getConnection(), 3);
      assertNotNull("Status should exist", status);
      dao.UpdateStatus(connexion.getConnection(), updateStatus);
      status = dao.getStatus(connexion.getConnection(), 3);
      assertEquals(status, updateStatus);
      
      
    } finally {
      closeConnection(connexion);
    }

  }
  
 /* @Test
  public void testGetAllStatus_PostgreSQL() throws Exception{
    IDatabaseConnection connexion = null;
    Status status1 = new Status(1, toDate(2010, Calendar.FEBRUARY, 01, 10, 34, 15), "je suis là");
    Status status2 = new Status(1, toDate(2010, Calendar.MARCH, 02, 10, 33, 10), "travaille tout simplement");
    Status status3 = new Status(1, toDate(2010, Calendar.JULY, 02, 10, 33, 10), "travaille sur readmine");
    status1.setId(1);
    status2.setId(2);
    status3.setId(4);
   
    int iduser = 1;
    try {
      connexion = getConnection();
    List<Status> status_list = dao.getAllStatus_PostgreSQL(connexion.getConnection(), iduser, 0, 2);
    assertNotNull("Status should exist", status_list);
    assertEquals("Should have 2 invitations in db", 2, status_list.size());
    assertEquals(status3, status_list.get(0));
    assertEquals(status2, status_list.get(1));
    
    } finally {
      closeConnection(connexion);
    }

    
  }*/
  
//  public void testGetAllStatus() throws Exception{
//    IDatabaseConnection connexion = null;
//    Status status1 = new Status(1, toDate(2010, Calendar.FEBRUARY, 01, 10, 34, 15), "je suis là");
//    Status status2 = new Status(1, toDate(2010, Calendar.MARCH, 02, 10, 33, 10), "travaille tout simplement");
//    Status status3 = new Status(1, toDate(2010, Calendar.JULY, 02, 10, 33, 10), "travaille sur readmine");
//    status1.setId(1);
//    status2.setId(2);
//    status3.setId(4);
//
//    int iduser = 1;
//    try {
//      connexion = getConnection();
//    List<Status> status_list = dao.getAllStatus(connexion.getConnection(), iduser, 0, 2);
//    assertNotNull("Status should exist", status_list);
//    assertEquals("Should have 2 invitations in db", 2, status_list.size());
//    assertEquals(status3, status_list.get(0));
//    assertEquals(status2, status_list.get(1));
//
//    } finally {
//      closeConnection(connexion);
//    }
//
//  }

  @Override
  protected String getDatasetFileName() {
    // TODO Auto-generated method stub
    return "socialNetwork_Status-dataset.xml";
  }

  private Date toDate(int year, int month, int day, int hour, int minute, int second) {
    GregorianCalendar calendar = new GregorianCalendar(year, month, day, hour, minute, second);
    return calendar.getTime();

  }

}