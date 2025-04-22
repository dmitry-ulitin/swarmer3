import { Account } from './account';
import { User } from './user';
/* eslint-disable @typescript-eslint/naming-convention */

export interface Group {
    id: number;
    fullName: string;
    owner: boolean;
    coowner: boolean;
    shared: boolean;
    accounts: Account[];
    permissions: Permission[];
    deleted?: boolean;
    ownerEmail?: string;
    opdate?: string;
}

export interface Permission {
    user: User;
    readonly: boolean;
    admin: boolean;
}

const add = (balance: Map<string, {value: number, currency: string, scale: number}>, amount: Group[] | Account[] | Group | Account) => {
    if (amount instanceof Array) {
        for (const a of amount) {
            add(balance, a);
        }
    } else if (!!amount.deleted) {
        return balance;
    } else if ('accounts' in amount) {
        add(balance, amount.accounts);
    } else if ('scale' in amount && 'currency' in amount && amount.currency) {
        let a = balance.get(amount.currency)?.value || 0;
        a += amount.balance || 0;
        balance.set(amount.currency, {value: a, currency: amount.currency, scale: amount.scale || 2});
    }
    return balance;
};

export const total = (amount: Group[] | Account[] | Group | Account) => [...add(new Map<string, {value: number, currency: string, scale: number}>(), amount).values()];
