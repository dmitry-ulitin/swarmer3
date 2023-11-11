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


