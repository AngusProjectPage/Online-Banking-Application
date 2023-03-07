package uk.co.asepstrath.bank;

import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import kong.unirest.JsonNode;
import kong.unirest.json.JSONObject;
import kong.unirest.json.JSONArray;
import org.slf4j.Logger;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.*;
import java.util.*;

public class BankData {

    private final DataSource ds;
    private final Logger log;

    public BankData(DataSource dataSource, Logger logger) {
        ds = dataSource;
        log = logger;
    }

    /**
     * Check if API is available
     */
    public boolean getAPIStatus() {
        return Unirest.get("http://api.asep-strath.co.uk/api/Team8/transactions?PageSize=0").asJson().getStatus() == 200; // success, API is online
    }

    public HashMap<String, Object> getAccount(String id) {
        HashMap<String, Object> hm = new HashMap<>();

        // Check if API is available, if not, use local database
        if (getAPIStatus()) {
            //hm.put("accounts", getAccountsAPI());
            ArrayList<Account> accs = getAccountsAPI();
            for (Account acc : accs) {
                if (Objects.equals(acc.getId(), id)) {
                    hm.put("account", acc);
                    break;
                }
            }
            hm.put("dataOrigin", "API");
        } else {
            //hm.put("accounts", getAccountsSQL());
            ArrayList<Account> accs = getAccountsSQL();
            for (Account acc : accs) {
                if (Objects.equals(acc.getId(), id)) {
                    hm.put("account", acc);
                    break;
                }
            }
            hm.put("dataOrigin", "DB");
        }

        return hm;
    }


    /**
     * Get accounts from API or local database
     *
     * @return HashMap containing accounts and data origin
     */
    public HashMap<String, Object> getAccounts() {
        HashMap<String, Object> hm = new HashMap<>();

        // Check if API is available, if not, use local database
        if (getAPIStatus()) {
            hm.put("accounts", getAccountsAPI());
            hm.put("dataOrigin", "API");
        } else {
            hm.put("accounts", getAccountsSQL());
            hm.put("dataOrigin", "DB");
        }

        return hm;
    }

    /**
     * Get accounts from API
     *
     * @return ArrayList of accounts
     */
    public ArrayList<Account> getAccountsAPI() {
        HttpResponse<JsonNode> res = Unirest.get("http://api.asep-strath.co.uk/api/Team8/accounts").asJson();
        JSONArray jsonAccs = res.getBody().getArray();
        ArrayList<Account> accs = new ArrayList<>();

        for (int i = 0; i < jsonAccs.length(); i++) {
            JSONObject jsonAcc = jsonAccs.getJSONObject(i);
            accs.add(new Account(
                    jsonAcc.getString("id"),
                    jsonAcc.getString("name"),
                    BigDecimal.valueOf(jsonAcc.getDouble("balance")),
                    jsonAcc.getString("currency"),
                    jsonAcc.getString("accountType")
            ));
        }

        return accs;
    }

    /**
     * Get accounts from local database
     *
     * @return ArrayList of accounts
     */
    public ArrayList<Account> getAccountsSQL() {
        try (Connection connection = ds.getConnection()) {
            try (Statement stmt = connection.createStatement()) {
                ResultSet rs = stmt.executeQuery("SELECT * FROM accounts");
                ArrayList<Account> accs = new ArrayList<>();
                while (rs.next())
                    accs.add(new Account(
                            rs.getString("id"),
                            rs.getString("name"),
                            BigDecimal.valueOf(rs.getDouble("balance")),
                            rs.getString("currency"),
                            rs.getString("accountType")
                    ));

                return accs;
            }
        } catch (SQLException e) {
            log.error("Error getting accounts from SQL", e);
            return new ArrayList<>();
        }

    }

    public BigDecimal intialBalance(String userID) {
        ArrayList<Account> accounts = getAccountsSQL();
        BigDecimal intitalBalance = new BigDecimal(0);
        for(int i=0; i<accounts.size(); i++) {
            if(accounts.get(i).getId() == userID) {
                intitalBalance = accounts.get(i).getBalance();
            }
        }
        return intitalBalance;
    }


    public void applyAllTransactions() {
        ArrayList<TransactionInfo> transactionInfo = initialiseTransactionInfo();
        ArrayList<Transaction> allTransactions = getTransactionsSQL();
        ArrayList<Account> accounts = getAccountsSQL();

        Account withdrawAccount = null;
        Account depositAccount = null;

        // Sort transactions by timestamp
        Comparator<Transaction> timeStampComparator = Comparator.comparing(Transaction::getTimestamp);
        Collections.sort(allTransactions, timeStampComparator);
        // Apply transactions to every account
        for(Transaction t: allTransactions) {
            for (Account a : accounts) {
                if (a.getId().equals(t.getWithdrawAccount())) {
                    withdrawAccount = a;
                } else if (a.getId().equals(t.getWithdrawAccount())) {
                    depositAccount = a;
                }
            }
            // Make transaction
            if (withdrawAccount.getBalance().doubleValue() >= t.getAmount().doubleValue()) {
                for(TransactionInfo transactions: transactionInfo) {
                    if(Objects.equals(transactions.getId(), withdrawAccount.getId())) {
                        BigDecimal newBalance = transactions.getBalanceAfter().subtract(t.getAmount());
                        transactions.updateTransactionCount();
                        transactions.setBalance(newBalance);
                    } else if (Objects.equals(transactions.getId(), depositAccount.getId())) {
                        BigDecimal newBalance = transactions.getBalanceAfter().subtract(t.getAmount());
                        transactions.updateTransactionCount();
                        transactions.setBalance(newBalance);
                    }
                }
            }
        }
    }

   public ArrayList<TransactionInfo> initialiseTransactionInfo() {
        ArrayList<TransactionInfo> allTransactions = new ArrayList<>();
        ArrayList<Account> accounts = getAccountsSQL();
        for(Account a: accounts) {
            allTransactions.add(new TransactionInfo(a.getId(), a.getBalance()));
        }
        return allTransactions;
    }


    public BigDecimal getAccountFinalBalance(String userID) {
        ArrayList<Transaction> accountTransactions = getAccountTransactions(userID);
        BigDecimal currentBalance = intialBalance(userID);
        // Sort all transactions by timestamp
        Comparator<Transaction> timeStampComparator = Comparator.comparing(Transaction::getTimestamp);
        Collections.sort(accountTransactions, timeStampComparator);
        for(int i=0; i<accountTransactions.size(); i++) {
            if(accountTransactions.get(i).getWithdrawAccount() == userID) {

            } else {

            }
        }


        return currentBalance;
    }

    public ArrayList<Transaction> getAccountTransactions(String userID) {
        ArrayList<Transaction> transactions = getTransactionsSQL();
        ArrayList<Transaction> accountTransactions = new ArrayList<>();
        for(Transaction transaction: transactions) {
            if(Objects.equals(transaction.getWithdrawAccount(), userID) || Objects.equals(transaction.getDepositAccount(), userID)) {
                accountTransactions.add(transaction);
            }
        }
            return accountTransactions;
    }

    /**
     * Get transactions from local database
     *
     * @return HashMap containing transactions and data origin
     */
    public HashMap<String, Object> getTransactions() {
        HashMap<String, Object> hm = new HashMap<>();
        ArrayList<Transaction> transactions = getTransactionsSQL();
        hm.put("transactions", transactions);
        if (transactions != null) hm.put("transactionTotal", transactions.size());
        hm.put("dataOrigin", "DB");
        return hm;
    }

    /**
     * Get transactions from API
     *
     * @return ArrayList of transactions
     */
    public ArrayList<Transaction> getTransactionsAPI() {
        ArrayList<Transaction> transactions = new ArrayList<>();
        int page = 1;
        while(page <= 3) {
            HttpResponse<JsonNode> res = Unirest.get("http://api.asep-strath.co.uk/api/Team8/transactions").queryString("PageNumber", page).asJson();
            JSONArray jsonTrans = res.getBody().getArray();
            if(res.getStatus() == 200) {
                for (int i=0; i < jsonTrans.length(); i++) {
                    JSONObject jsonT = jsonTrans.getJSONObject(i);
                    transactions.add(new Transaction(
                            jsonT.getString("id"),
                            jsonT.getString("depositAccount"),
                            jsonT.getString("withdrawAccount"),
                            jsonT.getString("timestamp"),
                            BigDecimal.valueOf(jsonT.getDouble("amount")),
                            jsonT.getString("currency")
                    ));
                }
                page++;
            } else {
                break;
            }
        }
        return transactions;
    }

    /**
     * Get transactions from local database
     *
     * @return ArrayList of transactions
     */
    public ArrayList<Transaction> getTransactionsSQL() {
        try (Connection connection = ds.getConnection()) {
            try (Statement stmt = connection.createStatement()) {
                ResultSet rs = stmt.executeQuery("SELECT * FROM transactions");
                ArrayList<Transaction> transactions = new ArrayList<>();
                while (rs.next())
                    transactions.add(new Transaction(
                            rs.getString("id"),
                            rs.getString("depositAccount"),
                            rs.getString("withdrawAccount"),
                            rs.getString("timestamp"),
                            BigDecimal.valueOf(rs.getDouble("amount")),
                            rs.getString("currency")
                    ));

                return transactions;
            }
        } catch (SQLException e) {
            log.error("Error getting transactions from SQL", e);
            return new ArrayList<>();
        }
    }

    /**
     * Sanitise SQL string
     *
     * @param s String to sanitise
     * @return Sanitised string
     */
    public String sanitiseSQL(String s) {
        return s.replace("'", "''");
    }

    /**
     * Post accounts to local database
     *
     * @param accs ArrayList of accounts
     */
    public void storeAccountsSQL(ArrayList<Account> accs) {
        try (Connection connection = ds.getConnection()) {
            try (Statement stmt = connection.createStatement()) {
                stmt.execute("CREATE TABLE IF NOT EXISTS accounts (id VARCHAR(36) PRIMARY KEY, name VARCHAR(255), balance DOUBLE, currency VARCHAR(3), accountType VARCHAR(255))");
                for (Account acc : accs) {

                    PreparedStatement pstmt = connection.prepareStatement("INSERT INTO accounts (id, name, balance, currency, accountType) VALUES (?, ?, ?, ?, ?)");

                    pstmt.setString(1, acc.getId());
                    pstmt.setString(2, sanitiseSQL(acc.getName()));
                    pstmt.setDouble(3, acc.getBalance().doubleValue());
                    pstmt.setString(4, acc.getCurrency());
                    pstmt.setString(5, acc.getAccountType());
                    pstmt.executeUpdate();
                }
            }
        } catch (SQLException e) {
            log.error("Error creating accounts table", e);
        }
    }

    /**
     * Post transactions to local database
     *
     * @param transactions ArrayList of transactions
     */
    public void storeTransactionsSQL(ArrayList<Transaction> transactions) {
        try (Connection connection = ds.getConnection()) {
            try (Statement stmt = connection.createStatement()) {
                stmt.execute("CREATE TABLE IF NOT EXISTS transactions (id VARCHAR(36) PRIMARY KEY, depositAccount VARCHAR(36), withdrawAccount VARCHAR(36), timestamp VARCHAR(255), amount DOUBLE, currency VARCHAR(3))");
                for (Transaction t : transactions) {

                    PreparedStatement pstmt = connection.prepareStatement("INSERT INTO transactions (id, depositAccount, withdrawAccount, timestamp, amount, currency) VALUES (?, ?, ?, ?, ?, ?)");

                    pstmt.setString(1, t.getId());
                    pstmt.setString(2, t.getDepositAccount());
                    pstmt.setString(3, t.getWithdrawAccount());
                    pstmt.setString(4, t.getTimestamp());
                    pstmt.setDouble(5, t.getAmount().doubleValue());
                    pstmt.setString(6, t.getCurrency());

                    pstmt.executeUpdate();

                }
            }
        } catch (SQLException e) {
            log.error("Error creating transactions table", e);
        }
    }

    /**
     * Initialise local database
     */
    public void initialise() {
        if (!getAPIStatus()) {
            log.error("API is not available, cannot initialise database");
            return;
        }

        // Get and store accounts/transactions
        storeAccountsSQL(getAccountsAPI());
        storeTransactionsSQL(getTransactionsAPI());
    }

}
