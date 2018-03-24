package ai.effect.token;

import java.math.BigInteger;
import java.util.Arrays;

import org.neo.smartcontract.framework.Appcall;
import org.neo.smartcontract.framework.SmartContract;
import org.neo.smartcontract.framework.Helper;
import org.neo.smartcontract.framework.services.neo.Storage;
import org.neo.smartcontract.framework.services.neo.Runtime;
import org.neo.smartcontract.framework.services.neo.TriggerType;
import org.neo.smartcontract.framework.services.neo.Blockchain;
import org.neo.smartcontract.framework.services.system.ExecutionEngine;

public class TokenLock extends SmartContract
{
    /* Address of the Effect Token contract */
    public static final String TOKEN_SCRIPT_HASH = "22285f37342e5b917be3548089d5096d7299335b";

    public static final String RET_NO_OP = "no op";
    public static final String ARG_ERROR = "arguments invalid";

    public static final String PREFIX_LOCK = "l";

    @Appcall(value=TOKEN_SCRIPT_HASH)
    public static Object token(String arg, Object[] args) {
        return null;
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
     * Get the total number of locked tokens
     */
    public static BigInteger getTotalLocked() {
        return (BigInteger) token("balanceOf", new Object[] {getAddress()});
    }

    /**
     * Get the number of tokens locked for `address` at `height`
     */
    public static BigInteger getLockedBalance(byte[] address, BigInteger height) {
        if (address.length != 20) return null;

        byte[] lockKey = storageKey(PREFIX_LOCK, address, height.toByteArray());

        return new BigInteger(Storage.get(Storage.currentContext(), lockKey));
    }


    /**
     * Returns the script hash of the contract
     */
    public static byte[] getAddress() {
        return ExecutionEngine.executingScriptHash();
    }

    /**
     * Send `amount` of tokens to an address that are locked until `height`
     */
    public static boolean lock(byte[] from, byte[] to, BigInteger value, BigInteger lockHeight) {
        if (value.compareTo(BigInteger.ZERO) <= 0) return false;
        if (to.length != 20) return false;
        if (lockHeight.intValue() <= Blockchain.height()) return false;

        byte[] lockAddress = getAddress();
        boolean transferred = (boolean) token("transfer", new Object[] {from, lockAddress, value});

        if (transferred == false) return false;

        byte[] lockKey = storageKey(PREFIX_LOCK, to, lockHeight.toByteArray());
        BigInteger lockValue = new BigInteger(Storage.get(Storage.currentContext(), lockKey));

        Storage.put(Storage.currentContext(), lockKey, lockValue.add(value));

        return true;
    }

    /**
     * Unlock all tokens locked for `to` at `height`
     */
    public static boolean unlock(byte[] to, BigInteger height) {
        if (height.intValue() > Blockchain.height()) return false;

        if (to.length != 20) return false;

        byte[] lockKey = storageKey(PREFIX_LOCK, to, height.toByteArray());
        BigInteger value = new BigInteger(Storage.get(Storage.currentContext(), lockKey));

        if (value.equals(BigInteger.ZERO)) return false;

        byte[] lockAddress = getAddress();
        boolean transferred = (boolean) token("transfer", new Object[] {lockAddress, to, value});

        if (transferred == false) return false;

        Storage.delete(Storage.currentContext(), lockKey);

        return true;
    }

    /**
     * Smart contract entrypoint
     */
    public static Object Main(String operation, Object[] args) {
        if (Runtime.trigger() == TriggerType.Verification) {
            return true;
        } else if (Runtime.trigger() == TriggerType.Application) {
            if (operation == "address") return getAddress();
            if (operation == "totalLocked") return getTotalLocked();

            if (operation == "lockedBalanceAt") {
                if (args.length != 2) return ARG_ERROR;

                byte[] address = (byte[]) args[0];
                BigInteger height = (BigInteger) args[1];
                return getLockedBalance(address, height);
            }

            if (operation == "lock") {
                if (args.length != 4) return ARG_ERROR;

                byte[] from = (byte[]) args[0];
                byte[] to = (byte[]) args[1];
                BigInteger amount = (BigInteger) args[2];
                BigInteger lockHeight = (BigInteger) args[3];

                return lock(from, to, amount, lockHeight);
            }

            if (operation == "unlock") {
                if (args.length != 2) return ARG_ERROR;

                byte[] to = (byte[]) args[0];
                BigInteger height = (BigInteger) args[1];
                return unlock(to, height);
            }

            return RET_NO_OP;
        }

        return false;
    }
}
