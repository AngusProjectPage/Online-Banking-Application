package uk.co.asepstrath.bank;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

public class AccountTests {

    @Test
    @DisplayName("Create Account")
    public void createAccount(){
        Account a = new Account();
        Assertions.assertNotNull(a);
    }

    @Test
    @DisplayName("Value Initialisation")
    public void ZeroStart(){
        Account a = new Account();
        Assertions.assertNotNull(a.getId());
        Assertions.assertEquals("Default Account", a.getName());
        Assertions.assertEquals(0, a.getBalance());
        Assertions.assertEquals("GBP", a.getCurrency());
        Assertions.assertEquals("Current Account", a.getAccountType());
    }

    @Test
    @DisplayName("Adding Funds")
    public void AddingFunds(){
        Account a = new Account("John Smith", new BigDecimal(20));
        a.deposit(50);
        Assertions.assertEquals(70,a.getBalance());
    }

    @Test
    @DisplayName("Withdrawing Funds")
    public void SubtractFunds(){
        Account a = new Account("John Smith", new BigDecimal(40));
        a.withdraw(20);
        Assertions.assertEquals(20,a.getBalance());
    }

    @Test
    @DisplayName("Throw Overdraft")
    public void NoOverDraft(){
        Account a = new Account("John Smith", new BigDecimal(30));
        Assertions.assertThrows(ArithmeticException.class,()-> a.withdraw(100));
    }

    @Test
    @DisplayName("Deposit/Withdraw Calculation")
    public void SuperSaving(){
        Account a = new Account("John Smith", new BigDecimal(20));
        for(int i=0; i<5; i++){
            a.deposit(10);
        }
        for(int j=0;j<3; j++){
            a.withdraw(20);
        }
        Assertions.assertEquals(10,a.getBalance());
    }

    @Test
    @DisplayName("Decimal Balance")
    public void DepositWithPennies(){
        Account a = new Account("John Smith", new BigDecimal("5.45"));
        a.deposit(17.56);
        Assertions.assertEquals(23.01,a.getBalance());
    }

}