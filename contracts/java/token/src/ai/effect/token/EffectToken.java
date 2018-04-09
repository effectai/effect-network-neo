package ai.effect.token;

import java.math.BigInteger;
import java.util.Arrays;

import org.neo.smartcontract.framework.SmartContract;
import org.neo.smartcontract.framework.Helper;
import org.neo.smartcontract.framework.services.neo.Blockchain;
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

    public static final boolean ARG_ERROR = false;

    public static final String PREFIX_APPROVE = "a";
    public static final String PREFIX_BALANCE = "b";
    public static final String PREFIX_LOCK = "l";

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
            return;
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
     * Transfer tokens from `from` to `to`
     */
    public static boolean transfer(byte[] from, byte[] to, BigInteger value) {
        if (value.compareTo(BigInteger.ZERO) <= 0) return false;
        if (to.length != 20) return false;
        if (!Runtime.checkWitness(from)) return false;

        BigInteger fromValue = getBalance(from);

        if (fromValue.compareTo(value) < 0) return false;

        byte[] fromKey = storageKey(PREFIX_BALANCE, from);

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
    public static boolean transferFrom(byte[] originator, byte[] from, byte[] to, BigInteger value) {
        if (value.compareTo(BigInteger.ZERO) <= 0) return false;
        if (!Runtime.checkWitness(originator)) return false;

        byte[] allowanceKey = storageKey(PREFIX_APPROVE, from, originator);

        if (allowanceKey.length != 41) return false;

        BigInteger allowanceValue = Helper.asBigInteger(Storage.get(Storage.currentContext(), allowanceKey));

        if (allowanceValue.compareTo(value) < 0) return false;

        BigInteger fromValue = getBalance(from);

        if (fromValue.compareTo(value) < 0) return false;

        byte[] fromKey = storageKey(PREFIX_BALANCE, from);

        storageSubtractOrDelete(fromKey, fromValue, value);

        BigInteger toValue = getBalance(to);
        byte[] toKey = storageKey(PREFIX_BALANCE, to);
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
    public static boolean approve(byte[] owner, byte[] spender, BigInteger value) {
        if (value.compareTo(BigInteger.ZERO) <= 0) return false;
        if (!Runtime.checkWitness(owner)) return false;

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
     * Get the number of tokens locked for `address` at `time`
     */
    public static BigInteger getLockedBalance(byte[] address, BigInteger time) {
        if (address.length != 20) return null;

        byte[] lockKey = storageKey(PREFIX_LOCK, address, time.toByteArray());

        return Helper.asBigInteger(Storage.get(Storage.currentContext(), lockKey));
    }

    /**
     * Send `amount` of tokens to an address that are locked until `time`
     */
    public static boolean lock(byte[] from, byte[] to, BigInteger value, BigInteger lockTime) {
        if (value.compareTo(BigInteger.ZERO) <= 0) return false;
        if (to.length != 20) return false;
        if (from.length != 20) return false;
        if (lockTime.longValue() <= Runtime.time()) return false;
        if (!Runtime.checkWitness(from)) return false;

        BigInteger fromValue = getBalance(from);

        if (fromValue.compareTo(value) < 0) return false;

        byte[] fromKey = storageKey(PREFIX_BALANCE, from);

        storageSubtractOrDelete(fromKey, fromValue, value);

        byte[] lockKey = storageKey(PREFIX_LOCK, to, lockTime.toByteArray());
        BigInteger lockValue = Helper.asBigInteger(Storage.get(Storage.currentContext(), lockKey));
        Storage.put(Storage.currentContext(), lockKey, lockValue.add(value));

        Runtime.notify("lock", from, to, value, lockTime);

        return true;
    }

    /**
     * Unlock all tokens locked for `to` at `time`
     */
    public static boolean unlock(byte[] to, BigInteger time) {
        if (time.longValue() > Runtime.time()) return false;
        if (to.length != 20) return false;

        byte[] lockKey = storageKey(PREFIX_LOCK, to, time.toByteArray());
        BigInteger value = Helper.asBigInteger(Storage.get(Storage.currentContext(), lockKey));

        if (value.equals(BigInteger.ZERO)) return false;

        BigInteger toValue = getBalance(to);
        byte[] toKey = storageKey(PREFIX_BALANCE, to);

        Storage.put(Storage.currentContext(), toKey, toValue.add(value));

        Storage.delete(Storage.currentContext(), lockKey);

        Runtime.notify("unlock", to, value, time);

        return true;
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
            if (operation.equals("name")) return NAME;
            if (operation.equals("symbol")) return SYMBOL;
            if (operation.equals("totalSupply")) return totalSupply();
            if (operation.equals("deploy")) return deploy();
            if (operation.equals("decimals")) return (int) DECIMALS;

            if (operation.equals("balanceOf")) {
                if (args.length != 1) return ARG_ERROR;

                byte[] account = (byte[]) args[0];

                return getBalance(account);
            }

            if (operation.equals("transfer")) {
                if (args.length != 3) return ARG_ERROR;

                byte[] from = (byte[]) args[0];
                byte[] to = (byte[]) args[1];
                BigInteger amount = (BigInteger) args[2];

                return transfer(from, to, amount);
            }

            if (operation.equals("transferFrom")) {
                if (args.length != 4) return ARG_ERROR;

                byte[] originator = (byte[]) args[0];
                byte[] from = (byte[]) args[1];
                byte[] to = (byte[]) args[2];
                BigInteger amount = (BigInteger) args[3];

                return transferFrom(originator, from, to, amount);
            }

            if (operation.equals("approve")) {
                if (args.length != 3) return ARG_ERROR;

                byte[] owner = (byte[]) args[0];
                byte[] spender = (byte[]) args[1];
                BigInteger amount = (BigInteger) args[2];

                return approve(owner, spender, amount);
            }

            if (operation.equals("allowance")) {
                if (args.length != 2) return ARG_ERROR;

                byte[] owner = (byte[]) args[0];
                byte[] spender = (byte[]) args[1];

                return allowance(owner, spender);
            }

            if (operation.equals("lockedBalanceAt")) {
                if (args.length != 2) return ARG_ERROR;

                byte[] address = (byte[]) args[0];
                BigInteger time = (BigInteger) args[1];

                return getLockedBalance(address, time);
            }

            if (operation.equals("lock")) {
                if (args.length != 4) return ARG_ERROR;

                byte[] from = (byte[]) args[0];
                byte[] to = (byte[]) args[1];
                BigInteger amount = (BigInteger) args[2];
                BigInteger lockTime = (BigInteger) args[3];

                return lock(from, to, amount, lockTime);
            }

            if (operation.equals("unlock")) {
                if (args.length != 2) return ARG_ERROR;

                byte[] to = (byte[]) args[0];
                BigInteger time = (BigInteger) args[1];

                return unlock(to, time);
            }

            return ARG_ERROR;
        }

        // Handle direct deposit
        return false;
    }
}
