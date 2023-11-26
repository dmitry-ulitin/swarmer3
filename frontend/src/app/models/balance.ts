import { Account } from "./account";
import { Group } from "./group";

export interface Balance { [currency: string]: number }

const add = (balance: Balance, amount: Group[] | Account[] | Group | Account) => {
    if (amount instanceof Array) {
        for (const a of amount) {
            add(balance, a);
        }
    } else if (!!amount.deleted) {
        return balance;
    } else if ('accounts' in amount) {
        add(balance, amount.accounts);
    } else if ('currency' in amount && amount.currency) {
        let a = balance[amount.currency] || 0;
        a += amount.balance || 0;
        balance[amount.currency] = a;
    }
    return balance;
};

export const total = (amount: Group[] | Account[] | Group | Account) => add({}, amount);

export interface Amount { value: number; currency: string };