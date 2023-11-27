import { Account } from './account';
import { User } from './user';
/* eslint-disable @typescript-eslint/naming-convention */

export interface Group {
    id: number;
    fullname: string;
    is_owner: boolean;
    is_coowner: boolean;
    is_shared: boolean;
    accounts: Account[];
    permissions?: Permission[];
    deleted?: boolean;
    ownerEmail?: string;
    opdate?: string;
}

export interface Permission {
    user: User;
    is_readonly: boolean;
    is_admin: boolean;
}

const add = (balance: Map<string, number>, amount: Group[] | Account[] | Group | Account) => {
    if (amount instanceof Array) {
        for (const a of amount) {
            add(balance, a);
        }
    } else if (!!amount.deleted) {
        return balance;
    } else if ('accounts' in amount) {
        add(balance, amount.accounts);
    } else if ('currency' in amount && amount.currency) {
        let a = balance.get(amount.currency) || 0;
        a += amount.balance || 0;
        balance.set(amount.currency, a);
    }
    return balance;
};

export const total = (amount: Group[] | Account[] | Group | Account) => [...add(new Map<string, number>(), amount).entries()].map(e => ({ value: e[1], currency: e[0] }));
