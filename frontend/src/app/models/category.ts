/* eslint-disable @typescript-eslint/naming-convention */

import { TransactionType } from "./transaction";

export interface Category {
    id: number;
    name: string;
    fullName: string;
    level: number;
    type: TransactionType;
    parentId: number | null;
}
