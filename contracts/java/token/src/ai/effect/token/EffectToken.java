package ai.effect.token;

import java.math.BigInteger;
import java.util.Arrays;

import org.neo.smartcontract.framework.SmartContract;
import org.neo.smartcontract.framework.Helper;
import org.neo.smartcontract.framework.services.neo.Runtime;
import org.neo.smartcontract.framework.services.neo.Storage;
import org.neo.smartcontract.framework.services.neo.TriggerType;
import org.neo.smartcontract.framework.services.system.ExecutionEngine;

public class EffectToken extends SmartContract
{
    public static final String NAME = "Effect.AI Token";
    public static final String SYMBOL = "EFX";

    public static final byte DECIMALS = 8;
    public static final long NUM_TOKENS = 650000000;   // total number of tokens
    public static final long FACTOR = 100000000;       // decided by DECIMALS

    public static final String ARG_ERROR = "arguments invalid";
    public static final boolean RET_NO_OP = false;

    public static final String PREFIX_APPROVE = "a";
    public static final String PREFIX_BALANCE = "b";
    /**
     * Get the owner of the contract
     */
    public static byte[] getOwner() {
        byte[] owner = {(byte) 177, 6, 60,(byte)  194,(byte)  249,(byte)  203, 3, 47, 76,(byte)  201, 11,(byte)  213,(byte)  156,(byte)  131, 7,(byte)  197,(byte)  180,(byte)  182,(byte)  147, 37};
        return owner;
    }

    /**
     * Generate a byte-array storage key. The prefix must be a 1-byte
     * string used to prevent storage collisions; any additional byte
     * arrays are concatenated.
     */
    public static byte[] storageKey(String prefix, byte[]... args) {
        byte[] prefixArray = Helper.asByteArray(prefix);
        byte[] key = Helper.concat(prefixArray, args[0]);
        for (int i = 1; i < args.length; i++)
            key = Helper.concat(key, args[i]);
        return key;
    }

    /**
     * Subtract an integer value from a storage key. If the remaining
     * value is 0 the key is deleted.
     */
    public static void storageSubtractOrDelete(byte[] key, BigInteger oldValue, BigInteger subValue) {
        if (oldValue.equals(subValue)) {
            Storage.delete(Storage.currentContext(), key);
        } else {
            Storage.put(Storage.currentContext(), key, oldValue.subtract(subValue));
        }
    }

    /**
     * Get the total token supply
     */
    public static BigInteger totalSupply() {
        return BigInteger.valueOf(NUM_TOKENS * FACTOR);
    }

    /**
     * Get token balance for an address
     */
    public static BigInteger getBalance(byte[] address) {
        byte[] key = storageKey(PREFIX_BALANCE, address);
        return new BigInteger(Storage.get(Storage.currentContext(), key));
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
    public static boolean transfer(byte[] from, byte[] to, BigInteger value, byte[] caller) {
        if (value.compareTo(BigInteger.ZERO) <= 0) return false;
        if (to.length != 20) return false;
        if (!checkWitness(from, caller)) return false;

        BigInteger fromValue = getBalance(from);
        byte[] fromKey = storageKey(PREFIX_BALANCE, from);

        if (fromValue.compareTo(value) < 0) return false;

        storageSubtractOrDelete(fromKey, fromValue, value);

        BigInteger toValue = getBalance(to);
        byte[] toKey = storageKey(PREFIX_BALANCE, to);
        Storage.put(Storage.currentContext(), toKey, toValue.add(value));

        Runtime.notify("transfer", from, to, value);

        return true;
    }

    /**
     * Transfer tokens on behalf of `from` to `to`, requires allowance
     */
    public static boolean transferFrom(byte[] originator, byte[] from, byte[] to, BigInteger value, byte[] caller) {
        if (value.compareTo(BigInteger.ZERO) <= 0) return false;
        if (!checkWitness(originator, caller)) return false;

        byte[] allowanceKey = storageKey(PREFIX_APPROVE, from, originator);

        if (allowanceKey.length != 41) return false;

        BigInteger allowanceValue = Helper.asBigInteger(Storage.get(Storage.currentContext(), allowanceKey));

        if (allowanceValue.compareTo(value) <= 0) return false;

        BigInteger fromValue = getBalance(from);
        byte[] fromKey = storageKey(PREFIX_BALANCE, from);

        if (fromValue.compareTo(value) < 0) return false;

        storageSubtractOrDelete(fromKey, fromValue, value);

        BigInteger toValue = getBalance(to);
        byte[] toKey = storageKey(PREFIX_BALANCE, from);
        Storage.put(Storage.currentContext(), toKey, toValue.add(value));

        storageSubtractOrDelete(allowanceKey, allowanceValue, value);

        Runtime.notify("transfer", from, to, value);

        return true;
    }

    /**
     * Set the amount that `spender` can send on behalf of `owner` to `value`
     *
     * This overwrites the any value
     */
    public static boolean approve(byte[] owner, byte[] spender, BigInteger value, byte[] caller) {
        if (value.compareTo(BigInteger.ZERO) <= 0) return false;
        if (!checkWitness(owner, caller)) return false;

        BigInteger ownerValue = getBalance(owner);

        if (ownerValue.compareTo(value) < 0) return false;

        byte[] approvalKey = storageKey(PREFIX_APPROVE, owner, spender);

        if (approvalKey.length != 41) return false;

        Storage.put(Storage.currentContext(), approvalKey, value);

        Runtime.notify("approve", owner, spender, value);

        return true;
    }

    /**
     * Get the amount that `spender` can send on behalf of `owner`
     */
    public static BigInteger allowance(byte[] owner, byte[] spender) {
        if (owner.length != 20) return null;
        if (spender.length != 20) return null;

        byte[] allowanceKey = storageKey(PREFIX_APPROVE, owner, spender);

        return Helper.asBigInteger(Storage.get(Storage.currentContext(), allowanceKey));
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

        if (initialized[0] == 0) {
            byte[] ownerKey = storageKey(PREFIX_BALANCE, owner);
            Storage.put(Storage.currentContext(), "initialized", BigInteger.ONE);
            Storage.put(Storage.currentContext(), ownerKey, totalSupply());
            return true;
        }

        return false;
    }

    /**
     * Smart contract entrypoint
     */
    public static Object Main(String operation, Object[] args) {
        if (Runtime.trigger() == TriggerType.Verification) {
            return Runtime.checkWitness(getOwner());
        } else if (Runtime.trigger() == TriggerType.Application) {
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
                if (args.length != 4) return ARG_ERROR;

                byte[] originator = (byte[]) args[0];
                byte[] from = (byte[]) args[1];
                byte[] to = (byte[]) args[2];
                BigInteger amount = (BigInteger) args[3];
                byte[] caller = (byte[]) ExecutionEngine.callingScriptHash();

                return transferFrom(originator, from, to, amount, caller);
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

        // Handle direct deposit
        return false;
    }
}
