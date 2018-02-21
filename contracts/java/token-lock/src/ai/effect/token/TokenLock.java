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

public class TokenLock extends SmartContract {
    /* Address of the Effect Token contract */
    public static final String TOKEN_SCRIPT_HASH = "22285f37342e5b917be3548089d5096d7299335b";

    public static final String RET_NO_OP = "no op";

    @Appcall(value = TOKEN_SCRIPT_HASH)
    public static Object token(String arg, Object[] args) {
        return null;
    }

    /**
     * Get the total number of locked tokens
     */
    public static BigInteger getTotalLocked() {
        return (BigInteger) token("balanceOf", new Object[]{getAddress()});
    }

    /**
     * Get the number of tokens locked for `address` at `height`
     */
    public static BigInteger getLockedBalance(byte[] address, BigInteger height) {
        byte[] lockKey = Helper.concat(address, height.toByteArray());
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
    public static Object lock(byte[] from, byte[] to, BigInteger value, BigInteger lockHeight) {
        if (value.compareTo(BigInteger.ZERO) <= 0) return "amount not positive";
        if (to.length != 20) return "invalid address";
        if (lockHeight.intValue() <= Blockchain.height())
            return "already unlocked";

        byte[] lockAddress = getAddress();
        boolean transferred = (boolean) token("transfer", new Object[]{from, lockAddress, value});
        if (transferred == false) return "transfer failed";

        byte[] lockKey = Helper.concat(to, lockHeight.toByteArray());
        BigInteger lockValue = new BigInteger(Storage.get(Storage.currentContext(), lockKey));

        Storage.put(Storage.currentContext(), lockKey, lockValue.add(value));

        return "success";
    }

    /**
     * Unlock all tokens locked for `to` at `height`
     */
    public static Object unlock(byte[] to, BigInteger height) {
        if (height.intValue() > Blockchain.height()) return "still locked";

        if (to.length != 20) return "invalid address";

        byte[] lockKey = Helper.concat(to, height.toByteArray());
        BigInteger value = new BigInteger(Storage.get(Storage.currentContext(), lockKey));
        if (value.equals(BigInteger.ZERO)) return "no balance";

        byte[] lockAddress = getAddress();
        boolean transferred = (boolean) token("transfer", new Object[]{lockAddress, to, value});
        if (transferred == false) return "transfer failed";

        Storage.delete(Storage.currentContext(), lockKey);

        return "success";
    }

    /**
     * Smart contract entrypoint
     */
    public static Object Main(String operation, Object[] args) {
        if (operation == "address") return getAddress();
        if (operation == "totalLocked") return getTotalLocked();

        if (operation == "lockedBalanceAt") {
            byte[] address = (byte[]) args[0];
            BigInteger height = (BigInteger) args[1];
            return getLockedBalance(address, height);
        }

        if (operation == "lock") {
            byte[] from = (byte[]) args[0];
            byte[] to = (byte[]) args[1];
            BigInteger amount = (BigInteger) args[2];
            BigInteger lockHeight = (BigInteger) args[3];

            return lock(from, to, amount, lockHeight);
        }

        if (operation == "unlock") {
            byte[] to = (byte[]) args[0];
            BigInteger height = (BigInteger) args[1];
            return unlock(to, height);
        }

        return RET_NO_OP;
    }
}
