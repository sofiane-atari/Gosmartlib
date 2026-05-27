export interface DataTableColumn {
  id: string;
  header: string;
  dataLabel?: string;
  align?: 'left' | 'center' | 'right';
  headerClass?: string;
  cellClass?: string;
}
