/* eslint-disable @typescript-eslint/naming-convention */

export interface Account {
    id: number;
    name: string;
    fullName: string;
    currency: string;
    chain: string;
    address: string;
    startBalance: number;
    balance?: number;
    deleted?: boolean;
    opdate?: string;
}
