import { Category } from './category';
import { Account } from './account';
import { Rule } from './rule';

export enum TransactionType {
    Transfer = 0,
    Expense,
    Income,
    Correction
}

interface TransactionBase {
    id: number;
    opdate: string;
    credit: number;
    debit: number;
    category?: Category | null;
    currency?: string;
    party?: string;
    details?: string;
    type: TransactionType;
}

interface TransactionTransfer extends TransactionBase {
    account: Account;
    recipient: Account;
}

interface TransactionExpense extends TransactionBase {
    account: Account;
    recipient: null;
}

interface TransactionIncome extends TransactionBase {
    account: null;
    recipient: Account;
}

export type Transaction = TransactionTransfer | TransactionExpense | TransactionIncome;

export type TransactionImport = Transaction & {
    catname?: string;
    selected: boolean;
    rule?: Rule;
}

export type TransactionView = Transaction & {
    amount: { value: number; currency: string; scale: number };
    balance: { aid: number; fullName: string; balance?: number; currency: string; scale: number };
};

