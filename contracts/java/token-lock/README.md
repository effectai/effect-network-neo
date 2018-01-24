# Effect Token Contract

A Token Lock for the Effect Token contract. This contract can transfer
tokens to a receiving address that are locked until a certain block
height.

## Specification

### Methods

#### address
`byte[]` Return the address of the token lock

#### totalLocked
`BigInteger` Return the total number of locked tokens

#### lockedBalanceAt (`byte[]` address, `BigInteger` height)
`BigInteger` Return the total number of locked tokens for an address at a height

#### lock (`byte[]` from, `byte[]` to, `BigInteger` value, `BigInteger` height)
`boolean` Lock tokens from an address, that can only be unlocked to a destination address at a certain block height

#### unlock (`byte[]` address, `BigInteger` height)
`boolean` Transfer all tokens were locked at height to the respective address
