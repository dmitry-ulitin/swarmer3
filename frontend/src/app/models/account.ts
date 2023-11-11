/* eslint-disable @typescript-eslint/naming-convention */

export interface Account {
    id: number;
    name: string;
    fullname: string;
    currency: string;
    start_balance: number;
    balance?: number;
    deleted?: boolean;
}
