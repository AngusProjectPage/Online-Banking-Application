package uk.co.asepstrath.bank;

import io.jooby.Jooby;
import io.jooby.ModelAndView;
import io.jooby.hikari.HikariModule;
import org.junit.jupiter.api.*;

import javax.sql.DataSource;

import org.slf4j.Logger;

import static org.mockito.Mockito.mock;

class BankControllerTests extends Jooby {

    static DataSource ds;
    static Logger log;
    static BankData data;

    @BeforeEach
    public void before() {
        install(new HikariModule("mem"));
        ds = require(DataSource.class);
        log = mock(Logger.class);

        data = new BankData(ds, log);
        data.initialise();
    }


    @Test
    @DisplayName("Create Controller")
    void createController() {
        BankController bankController = new BankController(data);
        Assertions.assertNotNull(bankController);
    }

    @Test
    @DisplayName("View Accounts")
    void viewAccounts() {
        BankController bankController = new BankController(data);
        ModelAndView mav = bankController.viewAccounts();
        Assertions.assertNotNull(mav.getModel().get("accounts"));
        Assertions.assertNotNull(mav.getModel().get("dataOrigin"));
    }

    @Test
    @DisplayName("View Account")
    void viewAccount() {
        BankController bankController = new BankController(data);
        ModelAndView mav = bankController.viewAccount("0126169a-2719-4ab2-8934-2b1fa33ec62e"); // arbitrary account, Evan Padberg
        Assertions.assertNotNull(mav.getModel().get("account"));
        Assertions.assertNotNull(mav.getModel().get("dataOrigin"));
    }

    @Test
    @DisplayName("View Transactions")
    void viewTransactions() {
        BankController bankController = new BankController(data);
        ModelAndView mav = bankController.viewTransactions();
        Assertions.assertNotNull(mav.getModel().get("transactions"));
        Assertions.assertNotNull(mav.getModel().get("transactionTotal"));
        Assertions.assertNotNull(mav.getModel().get("dataOrigin"));
    }

    @Test
    @DisplayName("View Transaction")
    void viewTransaction() {
        BankController bankController = new BankController(data);
        ModelAndView mav = bankController.viewTransaction("00059383-04c5-42ff-bfcc-1c3f88d55644"); // arbitrary transaction
        Assertions.assertNotNull(mav.getModel().get("transaction"));
        Assertions.assertNotNull(mav.getModel().get("dataOrigin"));
    }
}
