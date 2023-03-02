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

    @BeforeEach
    void before() {
        install(new HikariModule("mem"));
        ds = require(DataSource.class);
        log = mock(Logger.class);
    }

    @Test
    @DisplayName("Create Controller")
    void createController() {
        BankController bankController = new BankController(ds, log);
        Assertions.assertNotNull(bankController);
    }

    @Test
    @DisplayName("View Accounts")
    void viewAccounts() {
        BankController bankController = new BankController(ds, log);
        ModelAndView mav = bankController.viewAccounts();
        Assertions.assertNotNull(mav.getModel().get("accounts"));
        Assertions.assertNotNull(mav.getModel().get("dataOrigin"));
    }

    @Test
    @DisplayName("View Transactions")
    void viewTransactions() {
        BankController bankController = new BankController(ds, log);
        ModelAndView mav = bankController.viewTransactionInformation();
        Assertions.assertNotNull(mav.getModel().get("transactions"));
        Assertions.assertNotNull(mav.getModel().get("transactionTotal"));
        Assertions.assertNotNull(mav.getModel().get("dataOrigin"));
    }

}
