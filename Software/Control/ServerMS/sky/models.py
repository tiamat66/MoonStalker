from django.db import models

from django.utils.translation import ugettext_lazy as _


class SkyObject(models.Model):
    SKY_TYPES = (
        (0, _('ozvezdje')),
        (1, _('zvezda'))
    )

    name = models.CharField(max_length=32, verbose_name=_('Name'))
    type = models.IntegerField(verbose_name=_('Type'), choices=SKY_TYPES)

    def __str__(self):
        return "SkyObject:(%s, %s)" % (self.type, self.name)
