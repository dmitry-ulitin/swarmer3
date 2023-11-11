import { Category } from './category';
import { Account } from './account';
import { Rule } from './rule';

export enum TransactionType {
    Transfer = 0,
    Expense,
    Income,
    Correction
}

export interface Transaction {
    id?: number | null;
    opdate: string;
    account?: Account | null;
    credit: number;
    recipient?: Account | null;
    debit: number;
    category?: Category | null;
    currency?: string;
    party?: string;
    details?: string;
    type: TransactionType;
}

export interface TransactionImport extends Transaction {
    selected: boolean;
    rule: Rule;
}

