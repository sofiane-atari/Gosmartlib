import { TagGetOpt } from './tag-get-opt.model';
import { PageingOpt } from '../shared/paging/PageingOpt.model';

export interface TagPageRequest {
  tagGetOpt: TagGetOpt;
  pagingOpt: PageingOpt;
}