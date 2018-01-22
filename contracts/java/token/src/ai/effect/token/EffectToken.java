package ai.effect.token;

import java.math.BigInteger;
import java.util.Arrays;

import org.neo.smartcontract.framework.SmartContract;
import org.neo.smartcontract.framework.Helper;
import org.neo.smartcontract.framework.services.neo.Storage;
import org.neo.smartcontract.framework.services.neo.Runtime;
import org.neo.smartcontract.framework.services.system.ExecutionEngine;

public class EffectToken extends SmartContract
{
    public static final String NAME = "Effect";
    public static final String SYMBOL = "TBA";

    public static final byte DECIMALS = 8;
    public static final long NUM_TOKENS = 650000000;   // total number of tokens
    public static final long FACTOR = 100000000;       // decided by DECIMALS

    public static final String ARG_ERROR = "arguments invalid";
    public static final boolean RET_NO_OP = false;

    /**
     * Get the owner of the contract
     */
    public static byte[] getOwner() {
    	byte[] owner = {63, (byte) 201, 42, (byte) 180, 78, (byte) 141, 109, (byte) 223, (byte) 136, (byte) 187, (byte) 159, (byte) 170, 84, (byte) 129, (byte) 244, 77, (byte) 121, (byte) 127, 53, 88};
    	return owner;
    }

    /**
     * Get the total token supply
     */
    public static BigInteger totalSupply() {
    	return BigInteger.valueOf(NUM_TOKENS).multiply(BigInteger.valueOf(FACTOR));
    }

    /**
     * Get token balance for an address
     */
    public static BigInteger getBalance(byte[] address) {
    	return new BigInteger(Storage.get(Storage.currentContext(), address));
    }

    /**
     * Checks if transaction is signed by `from` or if it matches the
     * calling script.
     */
    private static boolean checkWitness(byte[] from, byte[] caller) {
	if (!Runtime.checkWitness(from)) {
	    if (!Arrays.equals(from, caller)) {
		return false;
	    }
	}
	return true;
    }

    /**
     * Transfer tokens from `from` to `to`
     * 
     * From can be a smart contract script hash, in which case it must
     * match `caller`.
     */
    public static Object transfer(byte[] from, byte[] to, BigInteger value, byte[] caller) {
    	if (value.compareTo(BigInteger.ZERO) < 0) return "negative value";
	if (!checkWitness(from, caller)) return "incorrect permission";

	BigInteger fromValue = getBalance(from);

    	if (fromValue.compareTo(value) < 0) return "no balance";

    	if (fromValue.equals(value)) {
    	    Storage.delete(Storage.currentContext(), from);
    	} else {
    	    Storage.put(Storage.currentContext(), from, fromValue.subtract(value));
    	}

    	BigInteger toValue = getBalance(to);
    	Storage.put(Storage.currentContext(), to, toValue.add(value));

    	Runtime.notify("transfer", from, to, value);

    	return true;
    }

    /**
     * Transfer tokens on behalf of `from` to `to`, requires allowance
     */
    public static Object transferFrom(byte[] from, byte[] to, BigInteger value) {
    	if (value.compareTo(BigInteger.ZERO) < 0) return "negative value";

	byte[] allowanceKey = Helper.concat(from, to);
	
	if (allowanceKey.length != 40) return false;
	
	BigInteger allowanceValue = getBalance(allowanceKey);

    	if (allowanceValue.compareTo(value) < 0) return "insufficient allowance";

	BigInteger fromValue = getBalance(from);

    	if (fromValue.compareTo(value) < 0) return "no balance";

    	if (fromValue.equals(value)) {
    	    Storage.delete(Storage.currentContext(), from);
    	} else {
    	    Storage.put(Storage.currentContext(), from, fromValue.subtract(value));
    	}

    	BigInteger toValue = getBalance(to);
    	Storage.put(Storage.currentContext(), to, toValue.add(value));

    	if (allowanceValue.equals(value)) {
    	    Storage.delete(Storage.currentContext(), allowanceKey);
    	} else {
    	    Storage.put(Storage.currentContext(), from, allowanceValue.subtract(value));
    	}

    	Runtime.notify("transfer", from, to, value);

    	return true;
    }

    /**
     * Set the amount that `spender` can send on behalf of `owner` to `value`
     *
     * This overwrites the any value
     */
    public static Object approve(byte[] owner, byte[] spender, BigInteger value, byte[] caller) {
    	if (value.compareTo(BigInteger.ZERO) < 0) return "negative value";
    	if (!checkWitness(owner, caller)) return "incorrect permission";

    	BigInteger ownerValue = getBalance(owner);

    	if (ownerValue.compareTo(value) < 0) return "no balance";

	byte[] approvalKey = Helper.concat(owner, spender);
	
	if (approvalKey.length != 40) return false;

    	Storage.put(Storage.currentContext(), approvalKey, value);

    	Runtime.notify("approve", owner, spender, value);

    	return true;
    }

    /**
     * Get the amount that `spender` can send on behalf of `owner`
     */
    public static BigInteger allowance(byte[] owner, byte[] spender) {
	byte[] allowanceKey = Helper.concat(owner, spender);
	
	if (allowanceKey.length != 40) return false;
	
	return getBalance(allowanceKey);
    }

    /**
     * Deploy smart contract
     *
     * Assign all tokens to the owner, can only be called once.
     */
    public static boolean deploy() {
    	byte[] owner = getOwner();
    	if (!Runtime.checkWitness(owner)) return false;

    	byte[] initialized = Storage.get(Storage.currentContext(), "initialized");
    	if (Arrays.equals(initialized, BigInteger.ZERO.toByteArray())) {
    	    Storage.put(Storage.currentContext(), "initialized", BigInteger.valueOf(1));
    	    Storage.put(Storage.currentContext(), owner, totalSupply());
    	    return true;
    	}
	
    	return false;
    }

    /** 
     * Smart contract entrypoint
     */
    public static Object Main(String operation,  Object[] args) {
	if (operation == "name") return NAME;
	if (operation == "symbol") return SYMBOL;
	if (operation == "totalSupply") return totalSupply();
    	if (operation == "deploy") return deploy();
    	if (operation == "decimals") return (int) DECIMALS;

    	if (operation == "balanceOf") {
    	    if (args.length != 1) return ARG_ERROR;
    	    byte[] account = (byte[]) args[0];
    	    return getBalance(account);
    	}

    	if (operation == "transfer") {
    	    if (args.length != 3) return ARG_ERROR;

    	    byte[] from = (byte[]) args[0];
    	    byte[] to = (byte[]) args[1];
    	    BigInteger amount = (BigInteger) args[2];
	    byte[] caller = (byte[]) ExecutionEngine.callingScriptHash();

    	    return transfer(from, to, amount, caller);
    	}

    	if (operation == "transferFrom") {
    	    if (args.length != 3) return ARG_ERROR;

    	    byte[] from = (byte[]) args[0];
    	    byte[] to = (byte[]) args[1];
    	    BigInteger amount = (BigInteger) args[2];

    	    return transferFrom(from, to, amount);
    	}

    	if (operation == "approve") {
    	    if (args.length != 3) return ARG_ERROR;

    	    byte[] owner = (byte[]) args[0];
    	    byte[] spender = (byte[]) args[1];
    	    BigInteger amount = (BigInteger) args[2];
	    byte[] caller = (byte[]) ExecutionEngine.callingScriptHash();

    	    return approve(owner, spender, amount, caller);
    	}

    	if (operation == "allowance") {
    	    if (args.length != 2) return ARG_ERROR;

    	    byte[] owner = (byte[]) args[0];
    	    byte[] spender = (byte[]) args[1];

    	    return allowance(owner, spender);
    	}

    	return RET_NO_OP;
    }

}
