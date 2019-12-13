package com.juzix.wallet.engine;


import org.web3j.crypto.Credentials;

import org.web3j.protocol.Web3j;
import org.web3j.protocol.Web3jFactory;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.response.PlatonBlock;
import org.web3j.protocol.core.methods.response.PlatonGasPrice;
import org.web3j.protocol.core.methods.response.PlatonGetBalance;
import org.web3j.protocol.core.methods.response.PlatonGetCode;
import org.web3j.protocol.core.methods.response.PlatonGetTransactionCount;
import org.web3j.protocol.core.methods.response.PlatonGetTransactionReceipt;
import org.web3j.protocol.core.methods.response.PlatonSendTransaction;
import org.web3j.protocol.core.methods.response.PlatonTransaction;
import org.web3j.protocol.core.methods.response.Transaction;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.protocol.http.HttpService;
import org.web3j.tx.RawTransactionManager;
import org.web3j.tx.TransactionManager;
import org.web3j.tx.gas.DefaultGasProvider;
import org.web3j.utils.Convert;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.concurrent.Callable;

import io.reactivex.Single;
import io.reactivex.functions.Function;

public class Web3jManager {

    private Web3j mWeb3j;

    private Web3jManager() {

    }

    public static Web3jManager getInstance() {
        return InstanceHolder.INSTANCE;
    }

    public void init(String url) {
        mWeb3j = Web3jFactory.build(new HttpService(url));
    }

    public Web3j getWeb3j() {
        return mWeb3j;
    }

    private TransactionManager getTransactionManager(Credentials credentials) {
        return new RawTransactionManager(mWeb3j, credentials);
    }

    public String getTransactionHash(Credentials credentials, BigInteger gasPrice, BigInteger gasLimit, String to,
                                     String data, BigInteger value) {
        String transactionHash = null;
        try {
            PlatonSendTransaction ethSendTransaction = getTransactionManager(credentials).sendTransaction(gasPrice, gasLimit, to, data, value);
            transactionHash = ethSendTransaction.getTransactionHash();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return transactionHash;
    }

    public double getBalance(String address) {
        try {
            PlatonGetBalance ethGetBalance = Web3jManager.getInstance().getWeb3j().platonGetBalance(address, DefaultBlockParameterName.LATEST).send();
            if (ethGetBalance != null && !ethGetBalance.hasError()) {
                return Convert.fromVon(new BigDecimal(ethGetBalance.getBalance()), Convert.Unit.LAT).doubleValue();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    public Transaction getTransactionByHash(String transactionHash) {
        try {
            PlatonTransaction ethTransaction = Web3jManager.getInstance().getWeb3j().platonGetTransactionByHash(transactionHash).send();
            Transaction transaction = ethTransaction.getTransaction();
            return transaction;
        } catch (Exception exp) {
            exp.printStackTrace();
        }
        return null;
    }

    public BigInteger getEstimateGas(String from, String to, String data) {
        try {
            return Web3jManager.getInstance().getWeb3j().platonEstimateGas(org.web3j.protocol.core.methods.request.Transaction.createEthCallTransaction(from, to, data)).send().getAmountUsed();
        } catch (Exception e) {
            e.printStackTrace();
            return BigInteger.ZERO;
        }
    }

    public BigInteger getNonce(String from) {

        PlatonGetTransactionCount ethGetTransactionCount = null;
        try {
            ethGetTransactionCount = Web3jManager.getInstance().getWeb3j().platonGetTransactionCount(
                    from, DefaultBlockParameterName.PENDING).send();
            if (ethGetTransactionCount.getTransactionCount().intValue() == 0) {
                ethGetTransactionCount = Web3jManager.getInstance().getWeb3j().platonGetTransactionCount(
                        from, DefaultBlockParameterName.LATEST).send();
            }
            return ethGetTransactionCount.getTransactionCount();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return BigInteger.ZERO;
    }

    public long getLatestBlockNumber() {
        try {
            PlatonBlock ethBlock = Web3jManager.getInstance().getWeb3j().platonGetBlockByNumber(DefaultBlockParameterName.LATEST, false).send();
            return ethBlock.getBlock().getNumber().longValue();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    public String getCode(String address) {
        try {
            PlatonGetCode code = Web3jManager.getInstance().getWeb3j().platonGetCode(address, DefaultBlockParameterName.LATEST).send();
            return code.getCode();
        } catch (Exception e) {
            e.printStackTrace();
            return "0x";
        }
    }

    public Single<BigInteger> getGasPrice() {
        return Single
                .fromCallable(new Callable<BigInteger>() {
                    @Override
                    public BigInteger call() {
                        BigInteger gasPrice = DefaultGasProvider.GAS_PRICE;
                        try {
                            PlatonGasPrice platonGasPrice = Web3jManager.getInstance().getWeb3j().platonGasPrice().send();
                            gasPrice = platonGasPrice.getGasPrice();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        return gasPrice;
                    }
                }).onErrorReturnItem(DefaultGasProvider.GAS_PRICE)
                .map(new Function<BigInteger, BigInteger>() {
                    @Override
                    public BigInteger apply(BigInteger bigInteger) throws Exception {
                        return getProcessedGasPrice(bigInteger);
                    }
                });
    }


    public TransactionReceipt getTransactionReceipt(String transactionHash) {
        TransactionReceipt transactionReceipt = null;
        try {
            PlatonGetTransactionReceipt ethGetTransactionReceipt =
                    mWeb3j.platonGetTransactionReceipt(transactionHash).send();
            if (!ethGetTransactionReceipt.hasError() && ethGetTransactionReceipt != null) {
                transactionReceipt = ethGetTransactionReceipt.getTransactionReceipt();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (transactionReceipt == null) {
            transactionReceipt = new TransactionReceipt();
        }

        return transactionReceipt;
    }

    /**
     * 获取处理过的gasPrice，保证后面10位为0
     *
     * @param oldGasPrice
     * @return
     */
    private BigInteger getProcessedGasPrice(BigInteger oldGasPrice) {
        BigDecimal bigDecimal = new BigDecimal(oldGasPrice).divide(BigDecimal.valueOf(10).pow(10));
        BigDecimal resultBigDecimal = bigDecimal.setScale(0, BigDecimal.ROUND_DOWN);
        //有小数位
        if (bigDecimal.compareTo(resultBigDecimal) == 1) {
            resultBigDecimal = resultBigDecimal.add(BigDecimal.ONE);
        }
        return resultBigDecimal.multiply(BigDecimal.valueOf(10).pow(10)).toBigInteger().max(new BigInteger(AppConfigManager.getInstance().getMinGasPrice()));
    }

    private static class InstanceHolder {
        private static volatile Web3jManager INSTANCE = new Web3jManager();
    }

}
