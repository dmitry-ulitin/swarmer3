/* eslint-disable @typescript-eslint/naming-convention */

export interface Account {
    id: number;
    name: string;
    fullName: string;
    currency: string;
    chain: string;
    address: string;
    scale: number;
    startBalance: number;
    balance?: number;
    deleted?: boolean;
    opdate?: string;
}

export const scale: { [key: string]: number } = { 'trc20': 6, 'erc20': 18, 'bep20': 18, 'btc': 8 };
